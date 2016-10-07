package com.mutsys.multipart;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

@Path("/upload")
public class MultipartResource {

	private final static Logger LOG = LoggerFactory.getLogger(MultipartResource.class);

	public static class UploadResponse {
		
		@JsonProperty
		private int numberOfFiles;
		
		@JsonProperty
		private List<String> filenames = new ArrayList<>();

		public int getNumberOfFiles() {
			return numberOfFiles;
		}

		public void setNumberOfFiles(int numberOfFiles) {
			this.numberOfFiles = numberOfFiles;
		}

		public void addFilename(String filename) {
			filenames.add(filename);
		}

	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadFile(MultiPart multiPartData) {
		
		UploadResponse uploadResponse = new UploadResponse();
		
		for (BodyPart bodyPart : multiPartData.getBodyParts()) {
			LOG.debug("--- being uploaded file info ---");
			//
			// basic info about the uploaded file
			//
			ContentDisposition contentDisposition = bodyPart.getContentDisposition();
			uploadResponse.addFilename(contentDisposition.getFileName());
			LOG.debug("mimeType: " + bodyPart.getMediaType());
			LOG.debug("fileName: " + contentDisposition.getFileName());
			LOG.debug("contentLength: " + contentDisposition.getSize());
			LOG.debug("lastModified: " + contentDisposition.getModificationDate());
			//
			// accessing the uploaded file in the temp directory it was uploaded to
			//
			File tempFile = bodyPart.getEntityAs(File.class);
			LOG.debug("temp file holding the upload: " + tempFile.getAbsolutePath());
			//
			// reading the bytes of the uploaded file as a stream
			//
			InputStream uploadedFileInputStream = bodyPart.getEntityAs(InputStream.class);
			BufferedInputStream bufferedFileInputStream = new BufferedInputStream(uploadedFileInputStream);
			byte[] fileBytes = new byte[1024];
			int bytesRead = 0;
			int totalBytesInUploadedFile = 0;
			try {
				while (-1 < (bytesRead = bufferedFileInputStream.read(fileBytes))) {
					totalBytesInUploadedFile += bytesRead;
					byte[] copyOfFileBytes = new byte[bytesRead];
					System.arraycopy(fileBytes, 0, copyOfFileBytes, 0, bytesRead);
				}
				bufferedFileInputStream.close();
			} catch (IOException e) {
				LOG.error("unable to read bytes from uploaded file", e);
			}
			LOG.debug("read " + totalBytesInUploadedFile + " bytes from uploaded file");
			LOG.debug("--- end uploaded file info ---");
		}
		
		uploadResponse.setNumberOfFiles(multiPartData.getBodyParts().size());
		
		return Response.ok(uploadResponse).build();
	}

}
