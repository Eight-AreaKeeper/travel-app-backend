package com.travelapp.customize.dto; import lombok.*; import java.time.LocalDateTime; import java.util.*;
@Getter @Builder @AllArgsConstructor public class SessionResponseDTO { private Long id; private String sessionToken; private Integer status; private List<DestinationInputDTO> destinations; private LocalDateTime createdAt; }
