package com.product.crud.Controller;

import com.product.crud.Exception.InvalidInputException;
import com.product.crud.Exception.UserAuthorizationException;
import com.product.crud.model.Image;
import com.product.crud.model.Product;
import com.product.crud.model.ResponseObject;
import com.product.crud.services.ProductService;
import com.product.crud.services.StorageService;
import com.timgroup.statsd.StatsDClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class StorageController {

    @Autowired
    private StorageService storageService;
    @Autowired
    private ProductService productService;

    @Autowired
    private StatsDClient statsDClient;
    Logger log = LoggerFactory.getLogger(StorageController.class);

    @RequestMapping(path = "/v1/product/{product_id}/image", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> uploadFile( @PathVariable("product_id") Integer product_id,@RequestParam(value="file")MultipartFile file, HttpServletRequest request){
        log.info("Inside Storage Controller. Uploading Image");
        statsDClient.incrementCounter("endpoint.uploadFile.http.post");
        try {
            if (!(productService.isAuthorisedForPut(product_id, request.getHeader("Authorization").split(" ")[1], null))) {
                throw new InvalidInputException("Invalid Username or Password");
            }
            return new ResponseEntity<Image>( storageService.uploadImage(file,product_id),HttpStatus.CREATED);


        }catch(InvalidInputException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            if(e.getMessage().matches("Invalid Username or Password")) return new ResponseEntity<ResponseObject>(response,HttpStatus.UNAUTHORIZED);
            return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
        }catch(UserAuthorizationException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.FORBIDDEN);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage("Error Occured while Uploading. Please make sure the file is not empty or currupt" + e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.FORBIDDEN);
        }

    }

    @RequestMapping(path = "/v1/product/download/{imageName}", method = RequestMethod.GET)
    public ResponseEntity<?> downloadImage(@PathVariable String imageName){

        log.info("Inside Storage Controller. Getting All Images");
        statsDClient.incrementCounter("endpoint.downloadImage.http.get");

        byte[] data = storageService.downloadImage(imageName);
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity
                .ok().contentLength(data.length)
                .header("Content-type","application/octet-stream")
                .header("Content-disposition","attachment;filename=\""+imageName+"\"")
                .body(resource);
    }

    @RequestMapping(path = "/v1/product/{product_id}/image/{image_id}", method = RequestMethod.GET)
    public ResponseEntity<?> getImageDetails(@PathVariable Integer product_id,@PathVariable Integer image_id,HttpServletRequest request){
        log.info("Inside Storage Controller. Getting Image");
        statsDClient.incrementCounter("endpoint.getImageDetails.http.get");

        try {
            if(productService.findProductById(product_id)<1){
                throw new InvalidInputException("Invalid Product ID");
            }

            if (!(productService.isAuthorisedForPut(product_id, request.getHeader("Authorization").split(" ")[1], null))) {
                throw new InvalidInputException("Invalid Username or Password");
            }
            //Authorise Product with ID
            if(!storageService.isAuthorisedWithProduct(image_id,product_id)){
                throw new InvalidInputException("Invalid Product ID for the Image");
            }
            Image image = storageService.getImage(image_id);
            if(image!=null){
                return new ResponseEntity<Image>( image,HttpStatus.OK);
            }else{
                return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
            }

        }catch(InvalidInputException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            if(e.getMessage().matches("Invalid Username or Password")) return new ResponseEntity<ResponseObject>(response,HttpStatus.UNAUTHORIZED);
            if(e.getMessage().matches("Invalid Product ID")) return new ResponseEntity<ResponseObject>(response,HttpStatus.NOT_FOUND);
            return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
        }catch(UserAuthorizationException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.FORBIDDEN);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(path = "/v1/product/{product_id}/image", method = RequestMethod.GET)
    public ResponseEntity<?> getImageList(@PathVariable Integer product_id,HttpServletRequest request){
        log.info("Inside Storage Controller. Getting Image");
        statsDClient.incrementCounter("endpoint.getImageList.http.get");

        try {
            if (!(productService.isAuthorisedForPut(product_id, request.getHeader("Authorization").split(" ")[1], null))) {
                throw new InvalidInputException("Invalid Username or Password");
            }
            List<Image> imageList = storageService.getAllImages(product_id);
            if(imageList!=null && !imageList.isEmpty()){
                return new ResponseEntity<List<Image>>( imageList,HttpStatus.OK);
            }else{
                return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
            }

        }catch(InvalidInputException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            if(e.getMessage().matches("Invalid Username or Password")) return new ResponseEntity<ResponseObject>(response,HttpStatus.UNAUTHORIZED);
            return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
        }catch(UserAuthorizationException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.FORBIDDEN);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.FORBIDDEN);
        }
    }




    @RequestMapping(path = "/v1/product/{product_id}/image/{image_id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<?> deleteImage(@PathVariable Integer image_id,@PathVariable Integer product_id,HttpServletRequest request){
        log.info("Inside Storage Controller. Getting Image");
        statsDClient.incrementCounter("endpoint.deleteImage.http.delete");
        try {
            if(productService.findProductById(product_id)<1){
                throw new InvalidInputException("Invalid Product ID");
            }
            if (!(productService.isAuthorisedForPut(product_id, request.getHeader("Authorization").split(" ")[1], null))) {
                throw new InvalidInputException("Invalid Username or Password");
            }
            if(!storageService.isAuthorisedWithProduct(product_id,image_id)){
                throw new InvalidInputException("Invalid Product ID for the Image");
            }
            String deleteResponse=storageService.deleteImage(image_id);
            if(deleteResponse.equals("Image Not Found"))  return new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
            return new ResponseEntity<String>( "Image Deleted Successfully",HttpStatus.NO_CONTENT);
        }catch(InvalidInputException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.BAD_REQUEST);
            response.setResponseMessage(e.getMessage());
            if(e.getMessage().matches("Invalid Username or Password")) return new ResponseEntity<ResponseObject>(response,HttpStatus.UNAUTHORIZED);
            if(e.getMessage().matches("Invalid Product ID")) return new ResponseEntity<ResponseObject>(response,HttpStatus.NOT_FOUND);
            return new ResponseEntity<ResponseObject>(response,HttpStatus.BAD_REQUEST);
        }catch(UserAuthorizationException e){
            ResponseObject response = new ResponseObject();
            response.setHttpStatusCode(HttpStatus.FORBIDDEN);
            response.setResponseMessage(e.getMessage());
            return new ResponseEntity<ResponseObject>(response,HttpStatus.FORBIDDEN);
        }
    }
}
