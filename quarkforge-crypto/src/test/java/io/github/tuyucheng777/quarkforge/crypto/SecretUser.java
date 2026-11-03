package io.github.tuyucheng777.quarkforge.crypto;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "qf_test_secret_user")
public class SecretUser extends PanacheEntity {

    @Convert(converter = EncryptedStringConverter.class)
    public String phone;
}
