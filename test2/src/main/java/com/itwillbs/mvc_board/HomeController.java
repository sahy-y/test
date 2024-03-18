package com.itwillbs.mvc_board;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
//	@RequestMapping(value = "/", method = RequestMethod.GET)
//	public String home(@RequestParam(defaultValue = "") String lang) {
////		return "home";
//		System.out.println(lang);
//		return "main";
//	}
	
	@GetMapping("/")
	public String home(@RequestParam(defaultValue = "ko-kr") String lang) {
//		return "home";
//		System.out.println("lang = " + lang);
		
		// 만약, lang 값이 "ko-kr" 일 경우 "main_ko-kr.jsp" 페이지로 포워딩하고
		// 아니면, "en-us" 일 경우 "main_en-us.jsp" 페이지로 포워딩
		if(lang.equals("ko-kr")) {
			return "main_ko-kr";
		} else if(lang.equals("en-us")) {
			return "main_en-us";
		} else {
			return "";
		}
	}
	
}











