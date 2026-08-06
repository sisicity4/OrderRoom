package com.github.karuhito.orderroombackend.resolver;

import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import com.github.karuhito.orderroombackend.entity.Participant;
import com.github.karuhito.orderroombackend.entity.Room;
import com.github.karuhito.orderroombackend.exception.InvalidHostKeyException;
import com.github.karuhito.orderroombackend.exception.InvalidHostKeyReason;
import com.github.karuhito.orderroombackend.exception.InvalidTokenException;
import com.github.karuhito.orderroombackend.exception.InvalidTokenReason;
import com.github.karuhito.orderroombackend.exception.RoomNotFoundException;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;


@Component
public class ParticipantArgumentResolver implements HandlerMethodArgumentResolver{
    private final ParticipantRepository participantRepository;

    public ParticipantArgumentResolver(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentParticipant.class)
                && parameter.getParameterType().equals(Participant.class);
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        
        
        Object attribute = webRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (attribute instanceof Map) {
            // Spring MVCがこの属性に必ずMap<String, String>を格納するため安全
            @SuppressWarnings("unchecked") Map<String,String> map = (Map<String, String>) attribute;
            
            // パス変数の Map から roomId を取得
            UUID roomId = UUID.fromString(map.get("roomId"));

            // "X-Participant-Token"を取得 -> null なら MISSING で例外
            String header = webRequest.getHeader("X-Participant-Token");
            if (header == null) {
                throw new InvalidTokenException(roomId, InvalidTokenReason.MISSING);
            }

            // UUID.fromStringでパース -> 失敗ならINVALID_FORMATで例外
            UUID token;
            try {
                token = UUID.fromString(header);
            } catch (IllegalArgumentException e) {
                throw new InvalidTokenException(roomId, InvalidTokenReason.INVALID_FORMAT);
            }
            // findByTokenAndRoomId で検索 -> 空なら MISMATCH で例外
            Participant participant = participantRepository.findByTokenAndRoomId(token, roomId).orElseThrow(() -> new InvalidTokenException(roomId, InvalidTokenReason.MISMATCH));
            // Participant を返す
            return participant;
        }
        throw new IllegalStateException("URIテンプレート変数が取得できません。{roomId}を含むパスのハンドラで@CurrentParticipantを使用してください");
    }    
    
}
