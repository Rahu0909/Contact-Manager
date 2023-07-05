package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.service.EmailService;

@Controller
public class ForgotController {
	Random random=new Random(1000);
	@Autowired
	private EmailService emailService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	//handler for email id form
	@GetMapping("/forgot")
	public String openForm(Model model) {
		model.addAttribute("title","Forgot Password");
		return "forget_email_form";
	}
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email,Model model,HttpSession session) {
		model.addAttribute("title","OTP Page");
		System.out.println("EMAIL "+email);
		
		//generating otp of 4 digit number
		int otp= random.nextInt(99999);
		System.out.println("OTP " +otp);
		
		//write otp for send otp to email
		String subject="OTP From SCM";
		String message=""
				+"<div style='border:1px solid #e2e2e2; padding:20px'>"
				+"<h1>"
				+"OTP is "
				+"<b>"+otp
				+"</b>"
				+"</h1>"
				+"</div>";
		String to=email;
		boolean flag = this.emailService.sendEmail(subject, message, to);
		if(flag) {
			session.setAttribute("myotp",otp);
			session.setAttribute("email", email);
			return "verify_otp";		
		}
		else {
			session.setAttribute("message", new Message("Check your given Email !!", "alert-success"));
			return "forget_email_form";
		}
	}
	
	//handler for verify otp
	@PostMapping("/verify-otp")
	public String verifyOTP(@RequestParam("otp") int otp,HttpSession session,Model model) {
		model.addAttribute("title","Verify-OTP");
		int myOtp=(int)session.getAttribute("myotp");
		String email=(String)session.getAttribute("email");
		if(myOtp==otp) {
			//password change form
			User user = this.userRepository.getUserByUserName(email);
			if(user==null) {
				//send error message
				session.setAttribute("message", new Message("User does not exist with this email!!", "alert-danger"));
				return "forget_email_form";
			}else {
				//send change password message
			}
			return "password_change_form";
		}else {
			session.setAttribute("message", new Message("You have entered wrong otp !!", "alert-danger"));
			return "verify_otp";
		}
	}
	
	//handler for change password after otp
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword") String newPassword,HttpSession session) {
		String email=(String)session.getAttribute("email");
		User user=this.userRepository.getUserByUserName(email);
		user.setPassword(this.passwordEncoder.encode(newPassword));
		this.userRepository.save(user);
		return "redirect:/signin?change=password changed successfully";
	}
}
