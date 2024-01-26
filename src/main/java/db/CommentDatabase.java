package db;

import com.google.common.collect.Maps;
import model.Comment;
import model.Qna;

import java.util.Collection;
import java.util.Map;

public class CommentDatabase {

    private static Long lastId = 1L;
    private static final Map<Long, Comment> comments = Maps.newHashMap();

    public static void add(Comment comment) {
        comment.setId(lastId);
        lastId += 1;

        comments.put(comment.getId(), comment);
    }

    public static Comment findById(Long id) {
        return comments.get(id);
    }

    public static Collection<Comment> findByQnaId(Long qnaId) {
        return comments.values().stream()
                .filter(comment -> comment.getQnaId().equals(qnaId))
                .toList();
    }

    public static Long countByQnaId(Long qnaId) {
        return comments.values().stream()
                .filter(comment -> comment.getQnaId().equals(qnaId))
                .count();
    }

    public static Collection<Comment> findAll() {
        return comments.values();
    }
}
