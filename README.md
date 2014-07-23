reactive-java8-play
===================

Blocking sample application for "Limmat Shopping". Base for refactoring towards reactive.

Content
-------

This applicaton is based on the Play! framework for Java8 Activator template.
Additional functionality is added to simulate a webshop with blocking subtasks such as
- get logon user data
- collect selected items (item-id and session-id in database)
- validate credit card for total amount
- make a reservation of the ordered items
- confirm credit card payment
- store an order on the filesystem
- send a confirmation email

Purpose
-------

The idea is to use this sample application for a refactoring workshop.

The blocking tasks should run in specific thread-pools to limit the number of blocking threads (e.g. all 
blocking database operations consume e.g. 4 threads).

Alteratively the subtasks can be rewritten to become non-blocking (e.g. the webservice call of the card acquirer
becomes non-blocking and returns a Future<Response>).

The processing of an order therefore needs rewriting to handle Futures. Alternatively akka actors can be used.
