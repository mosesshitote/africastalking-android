package com.africastalking.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.africastalking.Status;
import com.africastalking.models.payment.checkout.BankCheckoutRequest;
import com.africastalking.models.payment.checkout.BankCheckoutRequest.BankAccount;
import com.africastalking.models.payment.checkout.BankCode;
import com.africastalking.models.payment.checkout.CardCheckoutRequest;
import com.africastalking.models.payment.checkout.CardCheckoutRequest.PaymentCard;
import com.africastalking.models.payment.checkout.CheckoutRequest;
import com.africastalking.models.payment.checkout.CheckoutResponse;
import com.africastalking.models.payment.checkout.CheckoutValidateRequest;
import com.africastalking.models.payment.checkout.CheckoutValidationResponse;
import com.africastalking.services.PaymentService;
import com.africastalking.utils.Callback;

import xyz.belvi.luhn.Luhn;
import xyz.belvi.luhn.cardValidator.models.LuhnBank;
import xyz.belvi.luhn.cardValidator.models.LuhnCard;
import xyz.belvi.luhn.interfaces.LuhnCallback;
import xyz.belvi.luhn.interfaces.LuhnVerifier;
import io.card.payment.CardIOActivity;

public class Checkout {

    private static final int OTP_LENGTH = 4;

    private PaymentService paymentService;
    private CheckoutResponse checkoutResponse = null;


    public Checkout(PaymentService service) {
        this.paymentService = service;
    }

    private void handleCardCharge(CardCheckoutRequest request, LuhnCard paymentCard, final LuhnVerifier verifier, final Callback<CheckoutResponse> callback) {

        request.checkoutToken = null;
        request.paymentCard = new PaymentCard(
                paymentCard.getPan(),
                Integer.parseInt(paymentCard.getCvv()),
                paymentCard.getExpMonth(),
                paymentCard.getExpYear(),
                paymentCard.getCountryCode(),
                paymentCard.getPin()
        );

        paymentService.checkout(request, new Callback<CheckoutResponse>() {
            @Override
            public void onSuccess(CheckoutResponse data) {
                checkoutResponse = data;
                boolean success = data.status.contentEquals(Status.PENDING_VALIDATION);
                if (success) {
                    verifier.requestOTP(OTP_LENGTH);
                } else {
                    verifier.onDetailsVerified(false, "Payment Failed", data.description);
                    callback.onFailure(new Exception(data.description));
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                checkoutResponse = null;
                verifier.onDetailsVerified(false, "Payment Failed", throwable.getMessage() + "");
                callback.onFailure(throwable);
            }
        });

    }

    private void handleBankCharge(BankCheckoutRequest request, LuhnBank bank, final LuhnVerifier verifier, final Callback<CheckoutResponse> callback) {

        request.bankAccount = new BankAccount(
                bank.getAccountName(),
                bank.getAccountNumber(),
                BankCode.valueOf(bank.getBankCode())
        );
        request.bankAccount.dateOfBirth = bank.getDateOfBirth();

        paymentService.checkout(request, new Callback<CheckoutResponse>() {
            @Override
            public void onSuccess(CheckoutResponse data) {
                checkoutResponse = data;
                boolean success = data.status.contentEquals(Status.PENDING_VALIDATION);
                if (success) {
                    verifier.requestOTP(OTP_LENGTH);
                } else {
                    verifier.onDetailsVerified(false, "Payment Failed", data.description);
                    callback.onFailure(new Exception(data.description));
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                checkoutResponse = null;
                verifier.onDetailsVerified(false, "Payment Failed", throwable.getMessage() + "");
                callback.onFailure(throwable);
            }
        });

    }

    private void handleChargeVerification(CheckoutRequest.TYPE type, String otp, final LuhnVerifier verifier, final Callback<CheckoutResponse> callback) {

        if (checkoutResponse == null) {
            String message = "Invalid/Unexpected checkout response";
            verifier.onDetailsVerified(false, "Payment Failed", message);
            callback.onFailure(new Exception(message));
            return;
        }

        CheckoutValidateRequest validateRequest = new CheckoutValidateRequest();
        validateRequest.type = type;
        validateRequest.token = otp;
        validateRequest.transactionId = checkoutResponse.transactionId;
        paymentService.validateCheckout(validateRequest, new Callback<CheckoutValidationResponse>() {
            @Override
            public void onSuccess(CheckoutValidationResponse data) {
                boolean success = data.status.contentEquals(Status.SUCCESS);
                verifier.onDetailsVerified(success, "Payment Failed", data.description);
                if (success) {
                    CheckoutResponse response = new CheckoutResponse();
                    response.transactionId = checkoutResponse.transactionId;
                    response.checkoutToken = checkoutResponse.checkoutToken;
                    response.status = data.status;
                    response.description = data.description;
                    callback.onSuccess(response);
                } else {
                    callback.onFailure(new Exception(data.description));
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                checkoutResponse = null;
                verifier.onDetailsVerified(false, "Payment Failed", throwable.getMessage() + "");
                callback.onFailure(throwable);
            }
        });
    }

    public void start(final Activity context, final CheckoutRequest request, final Callback<CheckoutResponse> callback) {

        Luhn.LuhnType type;
        switch (request.type) {
            case CARD:
                type = Luhn.LuhnType.CARD;
                break;
            case BANK:
                type = Luhn.LuhnType.BANK;
                break;
            default:
                callback.onFailure(new Exception("Unsupported checkout type: " + request.type));
                return;
        }

        Bundle cardIoBundle = new Bundle();
        cardIoBundle.putBoolean(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false); // default: false
        cardIoBundle.putBoolean(CardIOActivity.EXTRA_SCAN_EXPIRY, false); // default: false
        cardIoBundle.putBoolean(CardIOActivity.EXTRA_REQUIRE_CVV, false); // default: false
        cardIoBundle.putBoolean(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false); // default: false
        cardIoBundle.putBoolean(CardIOActivity.EXTRA_HIDE_CARDIO_LOGO, true); // default: false
        cardIoBundle.putBoolean(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, false); // default: false


        Luhn.startLuhn(context, type, R.style.AfricasTalkingStyle, cardIoBundle, new LuhnCallback() {

            @Override
            public void bankDetailsRetrieved(Context luhnContext, LuhnBank bank, final LuhnVerifier verifier) {
                verifier.startProgress();
                handleBankCharge((BankCheckoutRequest) request, bank, verifier, callback);
            }

            @Override
            public void cardDetailsRetrieved(Context luhnContext, LuhnCard creditCard, final LuhnVerifier verifier) {
                verifier.startProgress();
                handleCardCharge((CardCheckoutRequest) request, creditCard, verifier, callback);
            }

            @Override
            public void otpRetrieved(Context luhnContext, final LuhnVerifier verifier, String otp) {
                handleChargeVerification(request.type, otp, verifier, callback);
            }

            @Override
            public void onFinished(boolean isVerified) { }
        });
    }
}