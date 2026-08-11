package com.github.karuhito.orderroombackend.resolver;

import com.github.karuhito.orderroombackend.repository.RoomRepository;

import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
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
import com.github.karuhito.orderroombackend.exception.UnauthenticatedException;
import com.github.karuhito.orderroombackend.repository.ParticipantRepository;

@Component
public class OperatorArgumentResolver implements HandlerMethodArgumentResolver {
    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;

    public OperatorArgumentResolver(ParticipantRepository participantRepository, RoomRepository roomRepository) {
        this.participantRepository = participantRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOperator.class)
                && parameter.getParameterType().equals(Operator.class);
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        Object attribute = webRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);
        if (attribute instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) attribute;

            UUID roomId = UUID.fromString(map.get("roomId"));

            // X-Host-Key ヘッダを読み込む
            String hostKeyString = webRequest.getHeader("X-Host-Key");
            // ヘッダがある場合
            if (hostKeyString != null) {
                // hostKeyをUUIDにパースする。失敗したらInvalidHostKeyExceptionをThrow
                UUID hostKeyUuid;
                try {
                    hostKeyUuid = UUID.fromString(hostKeyString);
                } catch (IllegalArgumentException e) {
                    throw new InvalidHostKeyException(roomId, InvalidHostKeyReason.MISMATCH);
                }
                Room room = roomRepository.findById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));
                if (!room.getHostKey().equals(hostKeyUuid)) {
                    throw new InvalidHostKeyException(roomId, InvalidHostKeyReason.MISMATCH);
                }
                Operator operator = new Operator(OperatorType.HOST, null);
                return operator;
                // ない場合
            } else {
                // X-Participant-Token ヘッダを読む
                String tokenString = webRequest.getHeader("X-Participant-Token");
                // ヘッダがある場合
                if (tokenString != null) {
                    // tokenStringをUUIDにパースする
                    UUID tokenUuid;
                    try {
                        tokenUuid = UUID.fromString(tokenString);
                        // パース失敗した場合
                    } catch (IllegalArgumentException e) {
                        throw new InvalidTokenException(roomId, InvalidTokenReason.INVALID_FORMAT);
                    }
                    Participant participant = participantRepository.findByTokenAndRoomId(tokenUuid, roomId)
                            .orElseThrow(() -> new InvalidTokenException(roomId, InvalidTokenReason.MISMATCH));
                    Operator operator = new Operator(OperatorType.PARTICIPANT, participant.getId());
                    return operator;
                } else {
                    throw new UnauthenticatedException(roomId);
                }
            }
        }
        throw new IllegalStateException("URIテンプレート変数が取得できません。{roomId}を含むパスのハンドラで@CurrentOperatorを使用してください");
    }
}