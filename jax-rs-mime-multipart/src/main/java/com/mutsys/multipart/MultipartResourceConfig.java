package com.mutsys.multipart;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.beans.factory.InitializingBean;

@ApplicationPath("/api")
public class MultipartResourceConfig extends ResourceConfig implements InitializingBean {
	
	private MultipartResource multipartResource;

	public void setMultipartResource(MultipartResource multipartResource) {
		this.multipartResource = multipartResource;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		register(JacksonFeature.class);
		register(MultiPartFeature.class);
		register(multipartResource);
		property(ServerProperties.TRACING, "ALL");
    	property(ServerProperties.TRACING_THRESHOLD, "VERBOSE");
	}

}
