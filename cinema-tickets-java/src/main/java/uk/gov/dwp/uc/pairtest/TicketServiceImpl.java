package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validatePurchaseRequest(accountId, ticketTypeRequests);

        int totalAmount = calculateTotalAmount(ticketTypeRequests);
        int totalSeatsToReserve = calculateSeatsToReserve(ticketTypeRequests);

        processPayment(accountId, totalAmount);
        reserveSeats(accountId, totalSeatsToReserve);
    }

     public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    private void validatePurchaseRequest(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account ID.");
        }

        int totalTickets = 0;
        int adultTickets = 0;
        int childTickets = 0;
        int infantTickets = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            int noOfTickets = request.getNoOfTickets();
            totalTickets += noOfTickets;

            switch (request.getTicketType()) {
                case ADULT:
                    adultTickets += noOfTickets;
                    break;
                case CHILD:
                    childTickets += noOfTickets;
                    break;
                case INFANT:
                    infantTickets += noOfTickets;
                    break;
            }
        }

        if (totalTickets > 25) {
            throw new InvalidPurchaseException("Cannot purchase more than 25 tickets.");
        }

        if ((childTickets > 0 || infantTickets > 0) && adultTickets == 0) {
            throw new InvalidPurchaseException("Child or Infant tickets cannot be purchased without at least one Adult ticket.");
        }
    }

    /**
     * Method to calculate the total amount for the tickets
     */
    private int calculateTotalAmount(TicketTypeRequest... ticketTypeRequests) {
        int totalAmount = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT:
                    totalAmount += request.getNoOfTickets() * 25; // Adult price
                    break;
                case CHILD:
                    totalAmount += request.getNoOfTickets() * 15; // Child price
                    break;
                case INFANT:// Infant ticket is free
                    break;
            }
        }

        return totalAmount;
    }

    /**
     * Method to calculate the total number of seats to reserve
     */
    private int calculateSeatsToReserve(TicketTypeRequest... ticketTypeRequests) {
        int totalSeatsToReserve = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT:
                case CHILD:
                    totalSeatsToReserve += request.getNoOfTickets();
                    break;
                case INFANT: // Infants do not need seats
                    break;
            }
        }

        return totalSeatsToReserve;
    }

    /**
     * Method to process the payment via the TicketPaymentService
     */
    private void processPayment(Long accountId, int totalAmount) {
        paymentService.makePayment(accountId, totalAmount);
    }

    /**
     * Method to reserve seats via the SeatReservationService
     */
    private void reserveSeats(Long accountId, int totalSeatsToReserve) {
        reservationService.reserveSeat(accountId, totalSeatsToReserve);
    }

}
