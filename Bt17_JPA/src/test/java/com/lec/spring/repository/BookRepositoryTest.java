package com.lec.spring.repository;

import com.lec.spring.domain.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void bookTest() {
        System.out.println("\n-- TEST#bookTest() ---------------------------------------------");
        Book book = new Book();
        book.setName("JPA 스터디");
        book.setAuthor("성연철");
        bookRepository.save(book);  // INSERT

        System.out.println(bookRepository.findAll());
        System.out.println("\n------------------------------------------------------------\n");
    }



}