# P2P Market 26.2

P2P Market is a Fabric mod for Minecraft 26.2 that adds a simple player-to-player trading terminal.

The mod is designed for multiplayer servers where players want to publish buy and sell orders, reserve trades, and contact each other directly to complete the exchange in-game.

## Features

- Sell orders
- Buy requests
- Personal order management
- Order reservations
- Live market updates
- Reservation status tracking
- Player-to-player contact via `/tell`
- English and Russian localization
- Automatic English fallback for other game languages
- `/trade` command to open the market terminal

## How it works

Players can create buy or sell orders with:

- Item name
- Amount
- Price per item

Other players can open the market, select an order, and reserve it.

The mod does not automatically transfer items or currency.

After reserving an order, both players are expected to contact each other and complete the trade manually in-game.

This makes the system suitable for roleplay and economy-focused servers where direct player interaction is important.

## Order States

Orders can have several states:

- Available
- Reserved
- Completed
- Closed

In the "My Orders" tab, reserved orders are shown first, followed by available orders, then completed or closed orders.

## Command

```text
/trade
