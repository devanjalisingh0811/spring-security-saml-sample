package com.demo.security.saml.spring.security;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author devanjali singh
 */
@Slf4j
@Service
public class SAMLUserDetailsServiceImpl implements SAMLUserDetailsService {

	public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
		log.info("Login received for user {}", credential.getNameID().getValue());
		return new SAMLUserDetails(credential);
	}
}
