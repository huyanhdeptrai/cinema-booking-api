// bộ lọc chặn và kiểm tra token
package com.cinema.booking.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    //hàm chặn mọi request đến server để kiểm tra jwt, chạy trước khi request đến được controller
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //1. trích xuất chuỗi jwt thô từ header authorization của request
        String jwt = getJwtFromRequest(request);

        //2. nếu có token và token hợp lệ, kiểu đúng chữ ký, chưa hết hạn
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            //3. giải mã lấy email từ token
            String email = tokenProvider.getEmailFromToken(jwt);
            //4. lấy toàn bộ UserDetails kèm role từ DB dựa vào email
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            //5. tạo đối tượng authentication hợp lệ (credentials để null vì dùng JWT, không cần password)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            //6. gắn thêm thông tin phụ từ request vào authentication, vd địa chỉ IP, user-agent
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            //7. lưu authentication vào SecurityContext đánh dấu request thành công, từ đây các controller, service có thể lấy thông tin user qua sercuritycontexholder
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        //8. cho phép request tiếp tục đi qua các filter tiếp theo
        filterChain.doFilter(request, response);
    }

    // hàm phụ tách chuỗi JWT ra khỏi header "Authorization", chuẩn gửi lên thường là "bearer eyJhbGciOi..."
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
