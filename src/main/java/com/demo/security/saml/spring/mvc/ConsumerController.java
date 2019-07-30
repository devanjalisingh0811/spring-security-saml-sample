package com.demo.security.saml.spring.mvc;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ConsumerController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerController.class);

	@RequestMapping("/saml/SSO")
	public ModelAndView catchAssertions(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		LOGGER.info("+++++++++++++++++++++++++++++++++++++");
		ModelAndView modelAndView = new ModelAndView("test");
		// modelAndView.addAllObjects(stringStringMap);
		return modelAndView;
	}
	
	
	@RequestMapping("/saml/logout")
	public ModelAndView logout(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		LOGGER.info("+++++++++++++++++++++++++++++++++++++");
		LOGGER.info("+++++++++++++++++++++++++++++++++++++");
		LOGGER.info("+++++++++++++++++++++++++++++++++++++");
		LOGGER.info("+++++++++++++++++++++++++++++++++++++");
		LOGGER.info("+++++++++++++++++++++++++++++++++++++");

		ModelAndView modelAndView = new ModelAndView("test");
		// modelAndView.addAllObjects(stringStringMap);
		return modelAndView;
	}
	
	@RequestMapping("/saml/SingleLogout")
	public ModelAndView SingleLogout(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		LOGGER.info("+++++++++++++++++++++++++++++++++++++");
		
		ModelAndView modelAndView = new ModelAndView("test");
		// modelAndView.addAllObjects(stringStringMap);
		return modelAndView;
	}
}
