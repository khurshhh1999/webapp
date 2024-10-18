package data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import models.User;

@Repository
public interface UserTestRepository extends JpaRepository<User, Long> {
    void deleteAll();
}