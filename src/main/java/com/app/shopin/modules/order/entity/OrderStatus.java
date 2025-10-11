package com.app.shopin.modules.order.entity;

public enum OrderStatus {
    PENDING,   // Orden creada, esperando pago
    PAID,      // Pago confirmado
    SHIPPED,   // Enviado al cliente
    DELIVERED, // Entregado
    CANCELLED  // Orden cancelada
}
