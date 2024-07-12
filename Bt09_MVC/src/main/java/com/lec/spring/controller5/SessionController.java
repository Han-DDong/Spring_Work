package com.lec.spring.controller5;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

@Controller
@RequestMapping("/session")
public class SessionController {


    // HttpSession 객체
    //  현재 request 한 client 에 대한 Session 정보
    @RequestMapping("/list")
    public void list(HttpSession session, Model model) {

        // 세션에 있는 '모든' attribute name 들 뽑아내기
        // name : String 타입
        Enumeration<String> enumeration = session.getAttributeNames();

        StringBuffer buff = new StringBuffer();
        int i = 0;
        while (enumeration.hasMoreElements()) {
            String sessionName = enumeration.nextElement();
            // session.getAttribute('name')  <-- 특정 세션 attr value 추출. 리턴타입 Object. 해당 name 이 없으면 null 리턴
            String sessionValue = session.getAttribute(sessionName).toString();
            buff.append((i + 1) + "] " + sessionName + " : " + sessionValue + "<br> ");
            i++;
        }
        if (i == 0) {
            buff.append("세선안에 attribute 가 없음.<br>"); // 여기서 attribute 는 name, value 쌍을 이야기 함
        }
        model.addAttribute("result", buff.toString());
    }

    @RequestMapping("/create")
    public String create(HttpSession session) {
        String sessionName, sessionValue;

        sessionName = "num1";
        sessionValue = "" + (int)(Math.random() * 100);

    // 세션 attr : name-value 생성
    // setAttribute(String name, Object value) 두번째 매개변수는 Object 타입이다
        session.setAttribute(sessionName, sessionValue);

        sessionName = "datetime";
        sessionValue = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        session.setAttribute(sessionName, sessionValue);

        return "redirect:/session/list";
    }


    @RequestMapping("/delete")
        public String delete(HttpSession session) {
        // removeAttribute(name) 세션 attribute 삭제
        session.removeAttribute("num1");

        return "redirect:/session/list";
    }

    // -----------------------------------------------------------------------------------------------------
    // session 로그인, 로그아웃
    public static final String ADMIN_ID = "admin";
    public static final String ADMIN_PW = "1234";

    @GetMapping("/login")
    public void login(HttpSession session, Model model){
        String username = (String) session.getAttribute("username");
        model.addAttribute("username", username);
    }

    @PostMapping("/login")
    public String loginOk(String username, String password, HttpSession session, Model model){
        if (username.equals(ADMIN_PW) && password.equals(ADMIN_PW)){
            session.setAttribute(ADMIN_ID, ADMIN_ID);
        } else {
            session.removeAttribute(ADMIN_ID);
        }
        return "session/loginOk";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session){
        session.removeAttribute(ADMIN_ID);
        return "session/logout";
    }

    // -----------------------------------------------------------------------------------------------------
    // session 로그인, 로그아웃
    // 강사님 코드


//    public static final String ADMIN_ID = "admin";
//    public static final String ADMIN_PW = "1234";
//
//    @GetMapping("/login")
//    public void login(HttpSession session, Model model){
//        // 현재 로그인 상태인지, 즉 로그인 세션 (name이 'username'인 세션값)이 있는지 확인
//        if(session.getAttribute("username") != null){
//            model.addAttribute("username", session.getAttribute("username"));
//        }
//    }
//
//    @PostMapping("/login")
//    public String loginOk(String username, String password, HttpSession session
//            , Model model){
//
//        // 세션 name-value 지정
//        String sessionName = "username";
//        String sessionValue = username;
//
//        // 제출된 id /pw 값이 일치하면 로그인 성공 + 세션 attr 생성
//        if(ADMIN_ID.equalsIgnoreCase(username) && ADMIN_PW.equals(password)){
//            session.setAttribute(sessionName, sessionValue);
//            model.addAttribute("result", true);
//        }else{
//            session.removeAttribute(sessionName);
//        }
//
//        return "session/loginOk";
//    }
//
//    @PostMapping("/logout")
//    public String logout(HttpSession session){
//
//        String sessionName = "username";
//
//        // 세션 삭제
//        session.removeAttribute(sessionName);
//
//        return "session/logout";
//    }


} // end controller
