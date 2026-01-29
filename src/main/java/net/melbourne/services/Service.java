package net.melbourne.services;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class Service {
    public String name;
    public String description;
}
