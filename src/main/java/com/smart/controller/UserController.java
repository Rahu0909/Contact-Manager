package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	// method to add common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
//		System.out.println("USERNAME"+userName);

		User user = userRepository.getUserByUserName(userName);
//		System.out.println("USER "+user);
		model.addAttribute("user", user);
		// get the user using username
	}

	// handler for dashboard home
	@GetMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// handler for add form
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// handler for processing contact
	@PostMapping("/process-contact")
	public String processContact(@Valid @ModelAttribute Contact contact, BindingResult result,
			@RequestParam("profileImage") MultipartFile file, Principal principal, Model model, HttpSession session) {
		try {

			if (result.hasErrors()) {
				System.out.println("ERROR" + result.toString());
				model.addAttribute("contact", contact);
				throw new Exception("You have not filled all entries correctly");
			}
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			// processing and uploading file
			if (file.isEmpty()) {
				// if the file is empty then try our message
				System.out.println("File is empty");
				contact.setImage("contact.png");
			} else {
				// save the file to folder and update the name and contact
				contact.setImage(file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			contact.setUser(user);
			user.getContacts().add(contact);

			this.userRepository.save(user);
			session.setAttribute("message", new Message("Successfully Added Contact!!", "alert-success"));
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("contact", contact);
			session.setAttribute("message", new Message("Somthing went wrong!!" + e.getMessage(), "alert-danger"));
			return "normal/add_contact_form";
		}
		return "normal/add_contact_form";
	}

	// handler for show contacts
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "Show User Contacts");

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		Pageable page1 = PageRequest.of(page, 6);
		// list
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), page1);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	// handler for particular contact details
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("CID " + cId);

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		// adding security
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_details";
	}

	// handler for delete contact
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session) {

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		// check...Assignment..
		this.contactRepository.delete(contact);

		session.setAttribute("message", new Message("Contact Deleted Successfully", "alert-success"));

		return "redirect:/user/show-contacts/0";
	}

	// handler for update contact
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model model) {

		model.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cid).get();
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}

	// handler for process update contact
	@PostMapping("/process-update")
	public String upateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session,Principal principal) {
		try {
			//old contact details
			Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				//file work
				
				//delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,oldcontactDetail.getImage());
				file1.delete();
				
				
				//update new photo
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			}
			else {
				contact.setImage(oldcontactDetail.getImage());
			}
			User user=this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Contact Updated Successfully..", "alert-success"));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		
		System.out.println("Contact name " + contact.getName());
		System.out.println("Contact ID " + contact.getcId());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//handler for your profile
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}

	//handler for settings
	@GetMapping("/settings")
	public String openSetting(Model model) {
		model.addAttribute("title","Settings Page");
		return "normal/settings";
	}
	
	//handler to process change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword,Principal principal,HttpSession session) {
		System.out.println("OLD PASSWORD " +oldPassword);
		System.out.println("NEW PASSWORD " +newPassword);
		
		String userName = principal.getName();
		User currentUser= this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());
		
		if(this.passwordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
			//change password
			currentUser.setPassword(this.passwordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Password Updated Successfully!!", "alert-success"));
		}
		else {
			//error
			session.setAttribute("message", new Message("Please Enter your correct old password", "alert-danger"));
			return "redirect:/user/settings";
		}
		
		return "redirect:/user/index";
	}

}
