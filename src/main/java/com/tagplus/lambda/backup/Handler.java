package com.tagplus.lambda.backup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Handler implements RequestHandler<S3Event, String> {
  private static final Logger logger = LoggerFactory.getLogger(Handler.class);

  @Override
  public String handleRequest(S3Event s3event, Context context) {
    try {
      String secret = System.getenv("secret");
      if (StringUtils.isNullOrEmpty(secret)) throw new InvalidParameterException("secret not found");

      final Path backup = exportFromFauna(secret);
      uploadToS3(backup);

      return "Ok";
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Path exportFromFauna(String token) throws IOException, InterruptedException {
    String source = "https://fauna-repo.s3.amazonaws.com/fdm/fdm.zip";
    String destination = "/tmp";
    String zipDestination = destination + "/fdm.zip";

    try {
      URL website = new URL(source);
      ReadableByteChannel rbc = Channels.newChannel(website.openStream());
      FileOutputStream fos = new FileOutputStream(zipDestination);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
      ZipFile zipFile = new ZipFile(zipDestination);
      zipFile.extractAll(destination);
    } catch (ZipException e) {
      e.printStackTrace();
    }

    Path fdm =
        Files.walk(Paths.get("/tmp")).filter(f -> f.toFile().isFile() && f.getFileName().toString().equals("fdm"))
        .findAny().map(Path::getParent).orElseThrow(FileNotFoundException::new);

    final Path backup = Files.createTempDirectory("backup");

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.command("bash", "fdm", "-source", "key=" + token, "-dest path=" + backup.toAbsolutePath().toString());
    processBuilder.directory(fdm.toFile());

    processBuilder.start().waitFor();
    return backup;
  }

  private void uploadToS3(Path backup) throws Exception {
    AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    String dstBucket = System.getenv("bucket");
    if (StringUtils.isNullOrEmpty(dstBucket)) throw new InvalidParameterException("bucket not found");

    String dstKey = String.format("fauna-%s", new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
    logger.info("Writing to: " + dstBucket + "/" + dstKey);

    Files.walk(backup).filter(f -> f.toFile().isFile()).forEach(f -> logger.info(f.toAbsolutePath().toString()));

    Files.walk(backup).filter(f -> f.toFile().isFile()).forEach(f -> {
      InputStream is;
      try {
        is = new DataInputStream(new FileInputStream(f.toFile()));
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(f.toFile().length());
        meta.setContentType("");
        s3Client.putObject(dstBucket, String.format("%s/%s", dstKey, f.getFileName()), is, meta);
        logger.info("Successfully uploaded to " + dstBucket + "/" + dstKey);
      } catch (FileNotFoundException e) {
        logger.error(e.getMessage());
        System.exit(1);
      }
    });
  }
}