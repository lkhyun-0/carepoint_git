package com.aws.carepoint.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FoodController {


        @GetMapping("/")
        public String diet() {
            return "foodRecord";
        }






    }

