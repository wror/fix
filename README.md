This is a Domain Driven Design model for the Financial Information eXchange (FIX®) Protocol.

It supports pooling order objects to avoid steady state allocation and deallocation, but is otherwise vanilla idiomatic java. Besides jsr330, it has no other dependencies.

The general design is that any business function can be invoked with a single method call, either to a repo or an entity. As well as the fundamental operations, statuses and quantites follow FIX. Multiple replace requests and multiple cancel request are not supported.

orderRepo.requestNew
requestRepo.requestReplace
requestRepo.requestCancel
request.accept
request.reject
order.done
order.cancel
order.fill
execution.correct
execution.bust

TODO: relationships between orders like parent-child and multileg
