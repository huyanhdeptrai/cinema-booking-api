// nạp User từ DB cho Spring Security
package com.cinema.booking.auth.security;

import com.cinema.booking.auth.entity.User;
import com.cinema.booking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // nhận chuỗi định danh, dùng email thay vì username, tìm kiếm user trong DB và nạp vào đối tượng UserDetails của Spring Security
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        //1. tìm user trong DB qua email, nếu không tìm thấy thì ném ngoại lệ UsernameNotFoundException
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        //2. định dạng role của user thành SimpleGrantedAuthority
        // Spring Security yêu cầu role phải có tiền tố "ROLE_"
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        //chuyển đổi entity User thành đối tượng UserDetails của Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(authority)
        );
    }
}