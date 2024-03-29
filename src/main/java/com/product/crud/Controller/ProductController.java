package com.product.crud.Controller;



import com.product.crud.Exception.DataNotFoundExeception;
import com.product.crud.Exception.InvalidInputException;
import com.product.crud.Exception.UserAuthorizationException;
import com.product.crud.Exception.UserExistException;

import com.product.crud.model.Product;
import com.product.crud.model.ResponseObject;
import com.product.crud.model.User;
import com.product.crud.services.CrudService;
import com.product.crud.services.ProductService;
import com.timgroup.statsd.StatsDClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class ProductController {

    @Autowired
    private CrudService service;
    @Autowired
    private ProductService productService;

    @Autowired
    private StatsDClient statsDClient;
    Logger log = LoggerFactory.getLogger(ProductController.class);
    ResponseObject responseObject;
    Map<String,String> responseHashmap;
    @RequestMapping(path = "/v1/product", method = RequestMethod.POST,produces="application/json")
    @ResponseBody
    public ResponseEntity<?> createProduct( @RequestBody Product product, HttpServletRequest request) {
        log.info("Inside Product Controller. Creating Product");
        statsDClient.incrementCounter("endpoint.createProduct.http.post");
        try {
            System.out.println(product.getOwner_user_id());
            if (product.getOwner_user_id()!=null  ) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Owner_user_id Cannot be passed");
            }
            Long userId = productService.authCredential(request.getHeader("Authorization").split(" ")[1]);
            if(userId == null){

                throw new UserAuthorizationException("Invalid Username or Password");
            }
            product.setOwner_user_id(userId);
            if (product == null || request == null) {
                throw new InvalidInputException("Request Body Cannot be Empty");
            }

            if (product.getName()==null || product.getName().isEmpty()) {
                throw new InvalidInputException("Product Name Cannot be Empty");

            }
            if (product.getDescription()==null || product.getDescription().isEmpty()) {
                throw new InvalidInputException("Product Description Cannot be Empty");

            }
            if (product.getSku()==null|| product.getSku().isEmpty()) {
                throw new InvalidInputException("Product SKU Cannot be Empty");

            }
            if(productService.ifProductSKUExists(product.getSku())) {
                throw new InvalidInputException("Product with SKU Exists");
            }
            if (product.getManufacturer().isBlank() || product.getManufacturer().isEmpty()) {
                throw new InvalidInputException("Manufacturer Cannot be Null");
            }
                try {
                    if ( product.getQuantity() < 0 || product.getQuantity() > 99) {
                        throw new InvalidInputException("Invalid Product Quantity");
                    }
                }catch(Exception e){
                    throw new InvalidInputException("Invalid Product Quantity");

                }
            if (product.getId()!=null ) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID Cannot be passed");
            }
            if (product.getDate_added()!=null || product.getDate_last_updated()!=null ) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Any Date Cannot be passed");
            }


            return new ResponseEntity<Product>( productService.addProduct(product),HttpStatus.CREATED);

        }
        catch(UserAuthorizationException e){

            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.UNAUTHORIZED);
        }
        catch(InvalidInputException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
        }
        catch(Exception e) {
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>( response,HttpStatus.BAD_REQUEST);

        }
    }


    @RequestMapping(path = "/v1/product/{productId}", method = RequestMethod.PUT,produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateProduct(@PathVariable Integer productId , @RequestBody Product product, HttpServletRequest request){
        log.info("Inside Product Controller. Updating Product");
        statsDClient.incrementCounter("endpoint.updateProduct.http.put");
        try{
            if(!(productService.isAuthorisedForPut(productId,request.getHeader("Authorization").split(" ")[1], product))){

                throw new InvalidInputException("Invalid Username or Password");

            }

            if (product == null || request==null) {
                throw new InvalidInputException("Product Name Cannot be Empty");
            }

            if (product.getName()==null || product.getName().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Product Name Cannot be Empty");
            }
            if (product.getDescription()==null || product.getDescription().isEmpty()) {
                throw new InvalidInputException("Product Description Cannot be Empty");
            }
            if (product.getSku()==null || product.getSku().isEmpty()) {
                throw new InvalidInputException("Product SKU Cannot be Empty");
            }

            if(productService.ifOtherProductWithSKUExists(product.getSku(),productId)) throw new UserExistException("User with SKU Exists");
            if (product.getManufacturer()==null || product.getManufacturer().isEmpty()) {
                throw new InvalidInputException("Manufacturer Cannot be Null");
            }
            try {
                if ( product.getQuantity() < 0 || product.getQuantity() > 99) {
                    throw new InvalidInputException("Invalid Product Quantity");

                }
            }catch(Exception e){
                throw new InvalidInputException("Invalid Product Quantity");
            }

            if (product.getId()!=null ) {
                throw new InvalidInputException("ID Cannot Be Passed");
            }
            if (product.getDate_added()!=null || product.getDate_last_updated()!=null ) {
                throw new InvalidInputException("Any Date Cannot Be Passed");
            }

            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.OK);
            response.setResponseMessage(productService.updateProduct(productId,product));
            return new ResponseEntity<ResponseObject>(response,HttpStatus.NO_CONTENT);

        }catch(UserAuthorizationException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.FORBIDDEN);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.FORBIDDEN);
        }
        catch(InvalidInputException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            if(e.getMessage().matches("Invalid Username or Password")) return new ResponseEntity<ResponseObject>(response,HttpStatus.UNAUTHORIZED);
            return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
        }
        catch(Exception e) {
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>( response,HttpStatus.BAD_REQUEST);
        }

    }

    @RequestMapping(path = "/v1/product/{productId}", method = RequestMethod.PATCH,produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateProductwithPatch(@PathVariable Integer productId , @RequestBody Product product, HttpServletRequest request){
        log.info("Inside Product Controller. Updating Product");
        statsDClient.incrementCounter("endpoint.updateProductwithPatch.http.patch");
        try{
            Product fromDb = productService.getProductbyId(productId);
            if(fromDb==null)
                return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
            if(!(productService.isAuthorisedForPut(productId,request.getHeader("Authorization").split(" ")[1], product))){
                throw new InvalidInputException("Invalid Username or Password");
            }

            String responseMessage = productService.updateProductwithPatch(productId,product);
            if(responseMessage.contains("Invalid")) throw new InvalidInputException(responseMessage);
            else if (responseMessage.equals("Another Product with SKU Exists")) throw new UserExistException(responseMessage);
            else if (responseMessage.equals("Product with the ID does not Exists")) throw new DataNotFoundExeception(responseMessage);

            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.OK);
            response.setResponseMessage(responseMessage);
            return new ResponseEntity<ResponseObject>(response,HttpStatus.NO_CONTENT);

        }
        catch(UserAuthorizationException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.FORBIDDEN);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.FORBIDDEN);
        }
        catch(InvalidInputException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            if(e.getMessage().matches("Invalid Username or Password")) return new ResponseEntity<ResponseObject>(response,HttpStatus.UNAUTHORIZED);
            return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
        }
        catch(UserExistException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
        }
        catch(DataNotFoundExeception e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.NOT_FOUND);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.NOT_FOUND);
        }
        catch(Exception e) {
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>( response,HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(path = "/v1/product/{productId}", method = RequestMethod.GET)
    public ResponseEntity<?> getProduct(@PathVariable Integer productId) {
        log.info("Inside Product Controller. Getting Product");
        statsDClient.incrementCounter("endpoint.getProduct.http.get");
        Product productFromDb = productService.getProductbyId(productId);
        if(productFromDb!=null){
            return new ResponseEntity<Product>( productFromDb,HttpStatus.OK);
        }else{
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.NOT_FOUND);
            response.setResponseMessage("Product with Id Does Not Exist");
            return new ResponseEntity<ResponseObject>(response,HttpStatus.NOT_FOUND);
        }

    }


    @RequestMapping(path = "/v1/product/{productId}", method = RequestMethod.DELETE)

    public ResponseEntity<?> deleteProduct(@PathVariable Integer productId , HttpServletRequest request) {
        log.info("Inside Product Controller. Deleting Product");
        statsDClient.incrementCounter("endpoint.deleteProduct.http.delete");
        try {
            Product fromDb = productService.getProductbyId(productId);
            if(fromDb==null)
                return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);

            if (!(productService.isAuthorisedForGet(productId, request.getHeader("Authorization").split(" ")[1]))) {
                throw new InvalidInputException("Invalid Username or Password");
            }

            String userName = productService.getUserIdFromToken(request.getHeader("Authorization").split(" ")[1]);
            productService.deleteImageByProductId(productId);
            String responseMessage = productService.deleteProduct(productId);

            return new ResponseEntity<>("Deleted", HttpStatus.NO_CONTENT);

        }catch(UserAuthorizationException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.FORBIDDEN);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.FORBIDDEN);
        }
    catch(Exception e){
        ResponseObject response = new ResponseObject();
        response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
        response.setResponseMessage(e.getMessage());
        if(e.getMessage().matches("Invalid Username or Password")) return new ResponseEntity<ResponseObject>(response,HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
    }
    }
}
