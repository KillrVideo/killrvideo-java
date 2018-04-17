package killrvideo.dao.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import killrvideo.entity.Comment;

public class CommentListResult {
    
    private List < Comment > comments = new ArrayList<>();
    
    private Optional < String > pagingState = Optional.empty();

    /**
     * Getter for attribute 'comments'.
     *
     * @return
     *       current value of 'comments'
     */
    public List<Comment> getComments() {
        return comments;
    }

    /**
     * Setter for attribute 'comments'.
     * @param comments
     * 		new value for 'comments '
     */
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    /**
     * Getter for attribute 'pagingState'.
     *
     * @return
     *       current value of 'pagingState'
     */
    public Optional<String> getPagingState() {
        return pagingState;
    }

    /**
     * Setter for attribute 'pagingState'.
     * @param pagingState
     * 		new value for 'pagingState '
     */
    public void setPagingState(Optional<String> pagingState) {
        this.pagingState = pagingState;
    }

}
