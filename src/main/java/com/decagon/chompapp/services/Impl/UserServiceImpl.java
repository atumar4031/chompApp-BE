package com.decagon.chompapp.services.Impl;



import com.decagon.chompapp.dtos.PasswordDto;
import com.decagon.chompapp.exceptions.PasswordConfirmationException;
import com.decagon.chompapp.dtos.EditUserDto;
import com.decagon.chompapp.dtos.EmailSenderDto;
import com.decagon.chompapp.dtos.ResetPasswordDto;
import com.decagon.chompapp.exceptions.UserNotFoundException;
import com.decagon.chompapp.models.User;
import com.decagon.chompapp.repositories.UserRepository;
import com.decagon.chompapp.security.CustomUserDetailsService;
import com.decagon.chompapp.security.JwtTokenProvider;
import com.decagon.chompapp.services.EmailSenderService;
import com.decagon.chompapp.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;


@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final EmailSenderService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<String> editUserDetails(EditUserDto editUserDto) {
        String person = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsernameOrEmail(person, person).get();
        if (editUserDto.getFirstName() != null)
            user.setFirstName(editUserDto.getFirstName());
        if (editUserDto.getLastName() != null)
            user.setLastName(editUserDto.getLastName());
        if (editUserDto.getUsername() != null)
            user.setUsername(editUserDto.getUsername());

        userRepository.save(user);

        String message = "User Details edit successful";

        return new ResponseEntity<>(message, HttpStatus.OK);
    }
    @Override
    public ResponseEntity<String> updatePassword(PasswordDto passwordDto) {

        if(!passwordDto.getNewPassword().equals(passwordDto.getConfirmPassword())){
            throw new PasswordConfirmationException("new password must be the same with confirm password");
        }

        User user = userRepository.findByEmail(SecurityContextHolder.getContext()
                        .getAuthentication().getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean matchPasswordWithOldPassword = passwordEncoder.matches(passwordDto.getOldPassword(), user.getPassword());

        if(!matchPasswordWithOldPassword){
            throw new PasswordConfirmationException("old password is not correct");
        }
        user.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.OK).body("password updated successfully");
    }


    @Override
    public String generateResetToken(String email) throws MessagingException {
        userRepository.findByEmail(email).orElseThrow(() ->
                new UserNotFoundException("User does not exits in the database"));

        String token = jwtTokenProvider.generateSignUpConfirmationToken(email);
        EmailSenderDto emailDto = new EmailSenderDto();
        emailDto.setTo(email);
        emailDto.setSubject("Reset Your Password");
        emailDto.setContent( "Please Use This Link to Reset Your Password\n" +
                "http://localhost:8081/api/v1/auth/users/enter-password?token=" + token);
        emailService.send(emailDto);
        return "Check Your Email to Reset Your Password";
    }
    @Override
    public ResponseEntity<String> enterResetPassword(String token, HttpServletResponse response){
        response.setHeader("Authorization", token);
        return ResponseEntity.ok("Please enter new password.");
    }


    @Override
    public String resetPassword(ResetPasswordDto resetPasswordDto, String token) {
        if (resetPasswordDto.getNewPassword().equals(resetPasswordDto.getConfirmNewPassword())) {
        String email = jwtTokenProvider.getUsernameFromJwt(token);

        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new UserNotFoundException("User does not exits in the database"));

        user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
        userRepository.save(user);
        return "Password Reset Successfully";
        }
        throw new RuntimeException("Passwords don't match.");
    }
}
