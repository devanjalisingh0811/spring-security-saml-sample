package com.demo.security.saml.spring.mvc;

/**
 * @author devanjali singh
 */

import com.demo.security.saml.spring.security.SAMLUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {

    @RequestMapping("/home")
    public ModelAndView home(@SAMLUser SAMLUserDetails user) {
        ModelAndView homeView = new ModelAndView("home");
        //homeView.addObject("userId", user.getUsername());
        //homeView.addObject("samlAttributes", user.getAttributes());
        return homeView;
    }

}
