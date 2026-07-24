package  com.github.karuhito.orderroombackend.interceptor;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.exception.InvalidHostKeyException;
import com.github.karuhito.orderroombackend.exception.InvalidHostKeyReason;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.repository.RoomRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class HostKeyInterceptor implements HandlerInterceptor {
    private final RoomRepository roomRepository;

    public HostKeyInterceptor(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

            String header = request.getHeader("X-Host-Key");

            Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (attribute instanceof Map) {
                // Spring MVCがこの属性に必ずMap<String, String>を格納するため安全
                @SuppressWarnings("unchecked") Map<String,String> map = (Map<String, String>) attribute;  
                UUID roomId = UUID.fromString(map.get("roomId"));
                if (header == null) {
                    throw new InvalidHostKeyException(roomId, InvalidHostKeyReason.MISSING);
                }
                Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
                
                try {
                    UUID checkHeader = UUID.fromString(header);
                    if (!room.getHostKey().equals(checkHeader)) {
                        throw new InvalidHostKeyException(roomId, InvalidHostKeyReason.MISMATCH);
                    }
                } catch (IllegalArgumentException e) {
                    throw new InvalidHostKeyException(roomId, InvalidHostKeyReason.MISMATCH);
                }
            }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}