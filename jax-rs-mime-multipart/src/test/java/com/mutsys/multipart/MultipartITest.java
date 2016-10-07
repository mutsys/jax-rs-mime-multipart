package com.mutsys.multipart;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.fasterxml.jackson.annotation.JsonProperty;

@RunWith(SpringRunner.class)
@TestExecutionListeners({
	DirtiesContextTestExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
})
@SpringBootTest(
	classes={Application.class},
	webEnvironment=WebEnvironment.RANDOM_PORT
)
@DirtiesContext
public class MultipartITest {
		
	@LocalServerPort
	private int port;
	
	//
	// create a MultiPart. this is the "envelope" for data you want to upload.
	// add a BodyPart to it for each item you want to upload.
	//
	private Supplier<MultiPart> supplier = () -> new MultiPart();
	//
	// convert a file to a FileDataBodyPart and add it to a MultiPart 
	//
	private BiConsumer<MultiPart,File> accumulator = (m,f) -> {
		//
		// do the conversion
		//
		FileDataBodyPart fileData = new FileDataBodyPart("label-image", f);
		BasicFileAttributeView fileAttributes = Files.getFileAttributeView(Paths.get(f.toURI()), BasicFileAttributeView.class);
		BasicFileAttributes basicFileAttributes = null;
		try {
			basicFileAttributes = fileAttributes.readAttributes();
			FormDataContentDisposition fileFormData = FormDataContentDisposition
					.name("label-image")
					.fileName(f.getName())
					.modificationDate(new Date(basicFileAttributes.lastModifiedTime().toMillis()))
					.size(basicFileAttributes.size())
					.build();
			fileData.contentDisposition(fileFormData);
		} catch (IOException e) { }
		//
		// add the converted file to the MultiPart
		//
		m.bodyPart(new FileDataBodyPart(f.getName(), f));
	};
	//
	// merge multiple MultiParts
	//
	private BiConsumer<MultiPart,MultiPart> combiner = (m1,m2) -> m2.getBodyParts().forEach(m1::bodyPart);
	
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
		
	@Test
	public void shouldUploadABunchOfImageFiles() throws Exception {
		//
		// read a bunch of files
		//
		Path imageDirectory = Paths.get(System.getProperty("user.dir"), "src", "test" , "resources");
		List<File> filesToUpload = Files.list(imageDirectory)
										.filter(p -> p.toString().endsWith(".jpg"))
										.map(p -> p.toFile())
										.collect(Collectors.toList());
		//
		// create a Jersey REST client and add the features that enable it to build
		// MultiPart data and send it to a server
		//
		Client client = ClientBuilder.newClient(
				new ClientConfig()
					.register(MultiPartFeature.class)
					.register(JacksonFeature.class)
					.register(MultiPartWriter.class)
			);
		//
		// convert the files into a MultiPart
		//
		MultiPart fileUploadData = filesToUpload.stream().collect(supplier, accumulator , combiner);
		//
		// tell the Jersey client where you want to send the uploaded files
		//
		WebTarget target = client.target(
				UriBuilder
					.fromUri("http://localhost")
					.port(port)
					.path("/api/upload")
					.build()
			);
		//
		// perform the upload
		//
		Response response = target.request().post(Entity.entity(fileUploadData, MediaType.MULTIPART_FORM_DATA));
		//
		// check to see if the response was successfull
		//
		assertThat(response, is(not(nullValue())));
		assertThat(response.getStatus(), is(equalTo(200)));
		//
		// check to see if we got back the data we expected from the service
		//
		UploadResponse returnedData = response.readEntity(UploadResponse.class);
		assertThat(returnedData, is(not(nullValue())));
		assertThat(returnedData.getNumberOfFiles(), is(equalTo(10)));
		
	}
	
}
