package devnatic.danceodyssey.DAO.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User implements UserDetails {
    @Id

    Long userID;
    String userName;
    String lastName;
    String email;
    String password;
    @Getter
    String userCV;
    @Getter
    boolean status = false;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    String confpassword;
    @JsonIgnore
    @ManyToMany(cascade = CascadeType.ALL)
    private Set<Event> eventsAttendedByUsers;

    @OneToMany(mappedBy = "userRec", cascade = CascadeType.ALL)
    private Set<Reclamation> userReclamations;
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private CART cart;
    @OneToMany(cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<RatingProduct> RatingProductsS;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }



    public boolean getStatus() {
        return status;
    }
}




