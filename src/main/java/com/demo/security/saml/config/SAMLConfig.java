package com.demo.security.saml.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Stream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLDiscovery;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.SAMLLogoutProcessingFilter;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.SAMLWebSSOHoKProcessingFilter;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.processor.HTTPArtifactBinding;
import org.springframework.security.saml.processor.HTTPPAOS11Binding;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.HTTPSOAP11Binding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.trust.httpclient.TLSProtocolConfigurer;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import com.demo.security.saml.certificate.KeystoreFactory;
import com.demo.security.saml.spring.SpringResourceWrapperOpenSAMLResource;
import com.demo.security.saml.spring.security.SAMLUserDetailsServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * @author devanjali singh
 */
@AutoConfigureBefore(WebSecurityConfig.class)
@Configuration
@Slf4j
public class SAMLConfig {

	@Autowired
	private SAMLUserDetailsServiceImpl samlUserDetailsServiceImpl;

	@Bean
	public SAMLAuthenticationProvider samlAuthenticationProvider() {
		SAMLAuthenticationProvider provider = new SAMLAuthenticationProvider();
		provider.setUserDetails(samlUserDetailsServiceImpl);
		provider.setForcePrincipalAsString(false);
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager() {
		return new ProviderManager(Collections.singletonList(samlAuthenticationProvider()));
	}

	@Bean(initMethod = "initialize")
	public StaticBasicParserPool parserPool() {
		return new StaticBasicParserPool();
	}

	@Bean
	public SAMLProcessorImpl processor() {
		HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
		ArtifactResolutionProfileImpl artifactResolutionProfile = new ArtifactResolutionProfileImpl(httpClient);
		HTTPSOAP11Binding soapBinding = new HTTPSOAP11Binding(parserPool());
		artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding));

		VelocityEngine velocityEngine = VelocityFactory.getEngine();
		Collection<SAMLBinding> bindings = new ArrayList<>();
		bindings.add(new HTTPRedirectDeflateBinding(parserPool()));
		bindings.add(new HTTPPostBinding(parserPool(), velocityEngine));
		bindings.add(new HTTPArtifactBinding(parserPool(), velocityEngine, artifactResolutionProfile));
		bindings.add(new HTTPSOAP11Binding(parserPool()));
		bindings.add(new HTTPPAOS11Binding(parserPool()));
		return new SAMLProcessorImpl(bindings);
	}

	@Bean
	public SimpleUrlLogoutSuccessHandler successLogoutHandler() {
		SimpleUrlLogoutSuccessHandler handler = new SimpleUrlLogoutSuccessHandler();
		handler.setDefaultTargetUrl("/");
		handler.setAlwaysUseDefaultTargetUrl(true);
		return handler;
	}

	@Bean
	public SecurityContextLogoutHandler logoutHandler() {
		SecurityContextLogoutHandler handler = new SecurityContextLogoutHandler();
		handler.setInvalidateHttpSession(true);
		handler.setClearAuthentication(true);
		return handler;
	}

	@Bean
	public SAMLLogoutFilter samlLogoutFilter() {
		SAMLLogoutFilter filter = new SAMLLogoutFilter(successLogoutHandler(), new LogoutHandler[] { logoutHandler() },
				new LogoutHandler[] { logoutHandler() });
		filter.setFilterProcessesUrl("/saml/logout");
		return filter;
	}

	@Bean
	public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
		SAMLLogoutProcessingFilter filter = new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
		filter.setFilterProcessesUrl("/saml/SingleLogout");
		return filter;
	}

	@Bean
	public MetadataGeneratorFilter metadataGeneratorFilter(MetadataGenerator metadataGenerator) {
		return new MetadataGeneratorFilter(metadataGenerator);
	}

	@Bean
	public MetadataDisplayFilter metadataDisplayFilter() throws Exception {
		MetadataDisplayFilter filter = new MetadataDisplayFilter();
		filter.setFilterProcessesUrl("/saml/metadata");
		return filter;
	}

	@Bean
	BeanFactoryPostProcessor idpMetadataLoader() {
		return beanFactory -> {
			PathMatchingResourcePatternResolver metadataFilesResolver = new PathMatchingResourcePatternResolver();
			try {
				Resource[] idpMetadataFiles = metadataFilesResolver.getResources("classpath:/idp-*.xml");
				Stream.of(idpMetadataFiles).forEach(idpMetadataFile -> {
					try {
						Timer refreshTimer = new Timer(true);
						ResourceBackedMetadataProvider delegate = null;
						delegate = new ResourceBackedMetadataProvider(refreshTimer,
								new SpringResourceWrapperOpenSAMLResource(idpMetadataFile));
						delegate.setParserPool(parserPool());
						ExtendedMetadata extendedMetadata = extendedMetadata().clone();
						ExtendedMetadataDelegate provider = new ExtendedMetadataDelegate(delegate, extendedMetadata);
						provider.setMetadataTrustCheck(true);
						provider.setMetadataRequireSignature(false);
						String idpFileName = idpMetadataFile.getFilename();
						String idpName = idpFileName.substring(idpFileName.lastIndexOf("idp-") + 4,
								idpFileName.lastIndexOf(".xml"));
						extendedMetadata.setAlias(idpName);
						beanFactory.registerSingleton(idpName, provider);
						log.info("Loaded Idp Metadata bean {}: {}", idpName, idpMetadataFile);
					} catch (Exception e) {
						throw new IllegalStateException("Unable to initialize IDP Metadata", e);
					}
				});
			} catch (Exception e) {
				throw new IllegalStateException("Unable to initialize IDP Metadata", e);
			}
		};
	}

	@Bean
	public ExtendedMetadata extendedMetadata() {
		ExtendedMetadata metadata = new ExtendedMetadata();
		// set flag to true to present user with IDP Selection screen
		metadata.setIdpDiscoveryEnabled(true);
		metadata.setRequireLogoutRequestSigned(true);
		// metadata.setRequireLogoutResponseSigned(true);
		metadata.setSignMetadata(false);
		return metadata;
	}

	@Bean
	public MetadataGenerator metadataGenerator(KeyManager keyManager) {
		MetadataGenerator generator = new MetadataGenerator();
		generator.setEntityId("localhost-demo");
		generator.setExtendedMetadata(extendedMetadata());
		generator.setIncludeDiscoveryExtension(false);
		generator.setKeyManager(keyManager);
		return generator;
	}

	@Bean(name = "samlWebSSOProcessingFilter")
	public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
		SAMLProcessingFilter filter = new SAMLProcessingFilter();
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationSuccessHandler(successRedirectHandler());
		filter.setAuthenticationFailureHandler(authenticationFailureHandler());
		filter.setFilterProcessesUrl("/saml/SSO");
		return filter;
	}

	@Bean
	public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {
		SAMLWebSSOHoKProcessingFilter filter = new SAMLWebSSOHoKProcessingFilter();
		filter.setAuthenticationSuccessHandler(successRedirectHandler());
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationFailureHandler(authenticationFailureHandler());
		return filter;
	}

	@Bean
	public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
		SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
		handler.setDefaultTargetUrl("/home");
		return handler;
	}

	@Bean
	public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
		SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler();
		handler.setUseForward(false);
		// handler.setDefaultFailureUrl("/error");
		return handler;
	}

	@Bean
	public SAMLDiscovery samlIDPDiscovery() {
		SAMLDiscovery filter = new SAMLDiscovery();
		filter.setFilterProcessesUrl("/saml/discovery");
		filter.setIdpSelectionPath("/idpselection");
		return filter;
	}

	@Bean
	public SAMLEntryPoint samlEntryPoint() {
		WebSSOProfileOptions options = new WebSSOProfileOptions();
		options.setIncludeScoping(false);
		SAMLEntryPoint entryPoint = new SAMLEntryPoint();
		entryPoint.setDefaultProfileOptions(options);
		entryPoint.setFilterProcessesUrl("/saml/login");
		return entryPoint;
	}

	@Bean
	public KeystoreFactory keystoreFactory(ResourceLoader resourceLoader) {
		return new KeystoreFactory(resourceLoader);
	}

	@Bean
	public KeyManager keyManager() {
		ClassPathResource storeFile = new ClassPathResource("/security/saml-demo.jks");
		String storePass = "devanjali";
		Map<String, String> passwords = new HashMap<>();
		passwords.put("devanjali", "devanjali");
		return new JKSKeyManager(storeFile, storePass, passwords, "devanjali");
	}

	@Bean
	public TLSProtocolConfigurer tlsProtocolConfigurer(KeyManager keyManager) {
		TLSProtocolConfigurer configurer = new TLSProtocolConfigurer();
		configurer.setKeyManager(keyManager);
		return configurer;
	}

}
