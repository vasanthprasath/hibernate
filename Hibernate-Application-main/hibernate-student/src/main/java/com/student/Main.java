package com.student;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class Main {

    public static void main(String[] args) {

        // Create SessionFactory
        SessionFactory factory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory();

        // Open Hibernate Session
        Session session = factory.openSession();

        try {

            // ==========================================
            // PART 1: INSERT STUDENT
            // ==========================================

            session.beginTransaction();

            Student student = new Student(
                    1,
                    "Vasanth prasath S",
                    "vasanthprasathsekar@gmail.com",
                    "AI & Data Science"
            );

            // Insert student into database
            session.persist(student);

            session.getTransaction().commit();

            System.out.println("=================================");
            System.out.println("Student inserted successfully!");
            System.out.println(student);
            System.out.println("=================================");

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            session.close();
        }

        // Close first SessionFactory
        factory.close();


        // ==========================================
        // PART 2: UPDATE STUDENT
        // ==========================================

        SessionFactory updateFactory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory();

        Session updateSession = updateFactory.openSession();

        try {

            updateSession.beginTransaction();

            // Find student using ID
            Student student = updateSession.get(Student.class, 1);

            if (student != null) {

                // Update student details
                student.setName("Vasanth prasath S");
                student.setEmail("vasanthprasathsekar@gmail.com");
                student.setCourse("Artificial Intelligence");

                System.out.println("=================================");
                System.out.println("Student found!");
                System.out.println("Updated Student:");
                System.out.println(student);
                System.out.println("=================================");

            } else {

                System.out.println("Student not found!");

            }

            // Commit update
            updateSession.getTransaction().commit();

            System.out.println("Student updated successfully!");

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            updateSession.close();
            updateFactory.close();
        }
    }
}