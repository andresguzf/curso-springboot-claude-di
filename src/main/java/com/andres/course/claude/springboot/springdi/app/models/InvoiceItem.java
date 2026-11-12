package com.andres.course.claude.springboot.springdi.app.models;

import java.math.BigDecimal;

public class InvoiceItem {

    private Product product;
    private Integer quantity;

    public InvoiceItem() {
    }

    public InvoiceItem(Product product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    // Total del item: precio del producto por la cantidad
    public BigDecimal getTotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
