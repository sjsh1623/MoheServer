package com.mohe.spring.dto;

import com.mohe.spring.entity.PlaceMenu;

/**
 * 장소 메뉴 DTO
 */
public class PlaceMenuDto {
    private Long id;
    private String name;
    private String price;
    private String description;
    private String imagePath;
    private Boolean isPopular;
    private Integer displayOrder;

    public PlaceMenuDto() {}

    public PlaceMenuDto(Long id, String name, String price, String description,
                        String imagePath, Boolean isPopular, Integer displayOrder) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.imagePath = imagePath;
        this.isPopular = isPopular;
        this.displayOrder = displayOrder;
    }

    public static PlaceMenuDto from(PlaceMenu menu) {
        return new PlaceMenuDto(
            menu.getId(),
            menu.getName(),
            menu.getPrice(),
            menu.getDescription(),
            menu.getImagePath(),
            menu.getIsPopular(),
            menu.getDisplayOrder()
        );
    }

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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Boolean getIsPopular() {
        return isPopular;
    }

    public void setIsPopular(Boolean popular) {
        isPopular = popular;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
