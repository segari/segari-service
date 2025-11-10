package id.segari.service.db.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fingerprint_adhoc_users")
public class FingerprintAdhocUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "internal_tools_user_id", nullable = false)
    private long internalToolsUserId;

    public FingerprintAdhocUser() {
    }

    public FingerprintAdhocUser(long id, long internalToolsUserId) {
        this.id = id;
        this.internalToolsUserId = internalToolsUserId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getInternalToolsUserId() {
        return internalToolsUserId;
    }

    public void setInternalToolsUserId(long internalToolsUserId) {
        this.internalToolsUserId = internalToolsUserId;
    }
}
