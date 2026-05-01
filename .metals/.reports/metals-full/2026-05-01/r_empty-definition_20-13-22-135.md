error id: file://<WORKSPACE>/app/src/main/java/os_p2/frontend/DedupResponseDto.java:java/util/List#
file://<WORKSPACE>/app/src/main/java/os_p2/frontend/DedupResponseDto.java
empty definition using pc, found symbol in pc: java/util/List#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 42
uri: file://<WORKSPACE>/app/src/main/java/os_p2/frontend/DedupResponseDto.java
text:
```scala
package os_p2.frontend;

import java.util.@@List;

/**
 * Design Pattern : Data Transfer Object (DTO).
 * Modélise la réponse globale attendue par la méthode accept().
 */
public record DedupResponseDto(
    String status,              // "success" ou "error: [message]"
    List<List<String>> groups   // Liste des groupes de doublons trouvés
) {}
```


#### Short summary: 

empty definition using pc, found symbol in pc: java/util/List#