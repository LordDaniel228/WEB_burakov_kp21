package lab2.lab2;

import org.springframework.web.bind.annotation.GetMapping; 
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentController {

    @GetMapping("/student/fullname")
    public String getFullname() {
        return "Burakov Daniil KP-21";
    }
}