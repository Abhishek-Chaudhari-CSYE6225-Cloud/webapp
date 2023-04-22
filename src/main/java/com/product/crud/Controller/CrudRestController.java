package com.product.crud.Controller;

import com.product.crud.Exception.DataNotFoundExeception;
import com.product.crud.Exception.InvalidInputException;
import com.product.crud.Exception.UserAuthorizationException;
import com.product.crud.Exception.UserExistException;
import com.product.crud.Validation.UserValidator;
import com.product.crud.errors.RegistrationStatus;
import com.product.crud.model.Product;
import com.product.crud.model.User;
import com.timgroup.statsd.StatsDClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import com.product.crud.services.CrudService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@RestController
public class CrudRestController {

	@Autowired
	private CrudService service;

	@Autowired
	private UserValidator userValidator;

	@InitBinder
	private void initBinder(WebDataBinder binder) {
		binder.setValidator(userValidator);
	}

	@Autowired
 	private StatsDClient statsDClient;
	Logger log = LoggerFactory.getLogger(CrudRestController.class);

	@RequestMapping(path = "/v1/user", method = RequestMethod.POST)
	public ResponseEntity<?> createUser( @RequestBody User user, HttpServletRequest request) {
		log.info("Inside User Controller. Creating User");
		statsDClient.incrementCounter("endpoint.createUser.http.post");
		User fromDBuser;
		try {
			if (user == null || request==null) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Request Body Cannot be Empty");
			}
			if (user.getUsername().isBlank() || user.getUsername().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User Name Cannot be Empty");
			}
			if (!user.getUsername().matches("^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$")) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Email Format");
			}
			System.out.println(user.getPassword());
			if (!(user.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$"))) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Password Format");
			}

			if(user.getId()!=null){
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot Enter Id");
			}
			if(user.getAccount_created()!=null){
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot Enter Account Created");
			}
			if(user.getAccount_updated()!=null){
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot Enter Account Updated");
			}

			fromDBuser = service.fetchUserByUserName(user.getUsername());
			if(fromDBuser==null){
				return ResponseEntity.status(HttpStatus.CREATED).body(service.saveUser(user));
			}else{
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User Already Exists");
			}

		}catch(Exception e){
			log.info(e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	@RequestMapping(path = "/v1/user/{userId}", method = RequestMethod.PUT)
	public ResponseEntity<?> updateUser(@PathVariable Long userId ,  @RequestBody User user,BindingResult errors, HttpServletRequest request ) {
		log.info("Inside User Controller. Updating User");
		statsDClient.incrementCounter("endpoint.updateUser.http.put");
		RegistrationStatus registrationStatus;
		if(user==null) return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Request Body Cannot be Empty");
		try {
			if (user == null || request==null) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Request Body Cannot be Empty");
			}
			if(user.getUsername()!=null){
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User Name Cannot be Entered");
			}

			System.out.println(user.getPassword());
			if (!(user.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$"))) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Password Format");
			}

			if(user.getId()!=null){
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot Enter Id");
			}
			if(user.getAccount_created()!=null){
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot Enter Account Created");
			}
			if(user.getAccount_updated()!=null){
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot Enter Account Updated");
			}
			service.isAuthorised(userId,request.getHeader("Authorization").split(" ")[1]);

			service.updateUser(user,userId);
			return ResponseEntity.status(HttpStatus.CREATED).body("User Updated");

		}
		catch (UserAuthorizationException e) {
			return new ResponseEntity<String>( e.getMessage(),HttpStatus.FORBIDDEN);
		}
		catch (DataNotFoundExeception e) {
			return new ResponseEntity<String>( e.getMessage(),HttpStatus.NOT_FOUND);
		}
		catch(Exception e) {
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(path = "/v2/user/{userId}", method = RequestMethod.GET)
	public ResponseEntity<?> fetchProductByID(@PathVariable Long userId, HttpServletRequest request) {
		log.info("Inside User Controller. Getting User");
		statsDClient.incrementCounter("endpoint.fetchProductByID.http.get");
		try {
			if(userId.toString().isBlank()||userId.toString().isEmpty()) {
				throw new InvalidInputException("Enter Valid User Id");
			}
			service.isAuthorised(userId,request.getHeader("Authorization").split(" ")[1]);
			return ResponseEntity.status(HttpStatus.OK).body(service.fetchUserbyId(userId));
		} catch (InvalidInputException e) {
			return new ResponseEntity<String>( e.getMessage(),HttpStatus.BAD_REQUEST);
		}
		catch (UserAuthorizationException e) {
			return new ResponseEntity<String>( e.getMessage(),HttpStatus.FORBIDDEN);
		}
		catch (DataNotFoundExeception e) {
			return new ResponseEntity<String>( e.getMessage(),HttpStatus.NOT_FOUND);
		}
		catch(Exception e) {
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(path = "/healthz", method = RequestMethod.GET)
	public void healthZ(HttpServletRequest request) {
		log.info("Healthz Good!!");
		statsDClient.incrementCounter("endpoint.healthZ.http.get");

	}
	
	@RequestMapping(path = "/health", method = RequestMethod.GET)
	public void health(HttpServletRequest request) {
		log.info("Health Check, not HealthZ, Good!!");
		statsDClient.incrementCounter("endpoint.health.http.get");

	}
}
