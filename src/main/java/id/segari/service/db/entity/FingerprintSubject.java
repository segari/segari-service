package id.segari.service.db.entity;

import id.segari.service.db.enums.TemplateGroup;
import id.segari.service.db.enums.TemplateVendor;
import jakarta.persistence.*;

@Entity
@Table(name = "fingerprint_subjects")
public class FingerprintSubject {

    @Id
    private long id;

    @Column(name = "internal_tools_user_id", nullable = false)
    private long internalToolsUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_group", nullable = false)
    private TemplateGroup templateGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_vendor", nullable = false)
    private TemplateVendor templateVendor;

    @Column(name = "template", nullable = false)
    private byte[] template;

    public FingerprintSubject() {
    }

    public FingerprintSubject(long id, long internalToolsUserId, TemplateGroup templateGroup,
                              TemplateVendor templateVendor, byte[] template) {
        this.id = id;
        this.internalToolsUserId = internalToolsUserId;
        this.templateGroup = templateGroup;
        this.templateVendor = templateVendor;
        this.template = template;
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

    public TemplateGroup getTemplateGroup() {
        return templateGroup;
    }

    public void setTemplateGroup(TemplateGroup templateGroup) {
        this.templateGroup = templateGroup;
    }

    public TemplateVendor getTemplateVendor() {
        return templateVendor;
    }

    public void setTemplateVendor(TemplateVendor templateVendor) {
        this.templateVendor = templateVendor;
    }

    public byte[] getTemplate() {
        return template;
    }

    public void setTemplate(byte[] template) {
        this.template = template;
    }
}
