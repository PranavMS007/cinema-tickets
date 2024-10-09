package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TicketServiceImplTest {
    private TicketPaymentService paymentService;
    private SeatReservationService reservationService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = Mockito.mock(TicketPaymentService.class);
        reservationService = Mockito.mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }


    @Test
    void testValidPurchaseWithMixedTickets() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        ticketService.purchaseTickets(1L, adultTicket, childTicket);

        verify(paymentService, times(1)).makePayment(1L, 65); // 2 adults (£50) + 1 child (£15) = £65
        verify(reservationService, times(1)).reserveSeat(1L, 3); // 2 adults + 1 child = 3 seats
    }

    @Test
    void testExceedsMaximumTickets() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, adultTicket);
        });

        verifyNoInteractions(paymentService);
        verifyNoInteractions(reservationService);
    }

    @Test
    void testChildTicketsWithoutAdult() {
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, childTicket);
        });

        verifyNoInteractions(paymentService);
        verifyNoInteractions(reservationService);
    }

    @Test
    void testInfantTicketsWithoutAdult() {
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, infantTicket);
        });

        verifyNoInteractions(paymentService);
        verifyNoInteractions(reservationService);
    }

    @Test
    void testValidPurchaseWithAdultAndInfant() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(1L, adultTicket, infantTicket);

        verify(paymentService, times(1)).makePayment(1L, 25); // 1 adult (£25), infant is free
        verify(reservationService, times(1)).reserveSeat(1L, 1); // Only 1 adult seat to reserve, infant on lap
    }

    @Test
    void testInvalidAccountId() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(null, adultTicket); // null account ID
        });

        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(0L, adultTicket); // account ID <= 0
        });

        verifyNoInteractions(paymentService);
        verifyNoInteractions(reservationService);
    }

    @Test
    void testValidPurchaseWithMaximumTickets() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
        TicketTypeRequest childTicket = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5);

        ticketService.purchaseTickets(1L, adultTicket, childTicket);

        verify(paymentService, times(1)).makePayment(1L, 575); // 20 adults (£500) + 5 children (£75) = £575
        verify(reservationService, times(1)).reserveSeat(1L, 25); // 20 adults + 5 children = 25 seats
    }

    @Test
    void testValidPurchaseWithOnlyAdults() {
        TicketTypeRequest adultTicket = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);

        ticketService.purchaseTickets(1L, adultTicket);

        verify(paymentService, times(1)).makePayment(1L, 75); // 3 adults (£25 each) = £75
        verify(reservationService, times(1)).reserveSeat(1L, 3); // 3 adults = 3 seats
    }

    @Test
    void testPurchaseWithOnlyInfants() {
        TicketTypeRequest infantTicket = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);

        assertThrows(InvalidPurchaseException.class, () -> {
            ticketService.purchaseTickets(1L, infantTicket);
        });

        verifyNoInteractions(paymentService);
        verifyNoInteractions(reservationService);
    }

}
