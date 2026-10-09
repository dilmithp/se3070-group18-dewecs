package com.group18.dewecs.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

/** A river basin and the districts it runs through. A flood warning can target every district of a basin at once. */
@Entity
@Table(name = "river_basins")
public class RiverBasin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "river_basin_districts",
            joinColumns = @JoinColumn(name = "river_basin_id"),
            inverseJoinColumns = @JoinColumn(name = "district_id")
    )
    private Set<District> districts = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<District> getDistricts() {
        return districts;
    }

    public void setDistricts(Set<District> districts) {
        this.districts = districts;
    }
}
