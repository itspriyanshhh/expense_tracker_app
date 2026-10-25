package com.example.pulseledger.domain.parser

import com.example.pulseledger.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionParserTest {

    @Test
    fun testHdfcBankUpiDebitSms() {
        val sms = "Dear Customer, INR 540.00 debited from A/c XX1234 on 24-OCT-26 to VPA swiggy@icici (Swiggy). UPI Ref: 429182749102."
        val result = TransactionParser.parse(sms, senderOrPackage = "VM-HDFCBK")

        assertNotNull(result)
        assertEquals(540.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Swiggy", result.rawMerchant)
        assertEquals("swiggy@icici", result.upiVpa)
        assertEquals("429182749102", result.upiRefNo)
        assertEquals("HDFC Bank", result.bankOrAppSource)
    }

    @Test
    fun testSbiBankingSms() {
        val sms = "Your A/C ending 4567 debited by Rs. 200.00 on 24/10/26 transfer to DMRC DELHI METRO Ref No 429182749104. Available Bal: Rs. 14,350.00."
        val result = TransactionParser.parse(sms, senderOrPackage = "AX-SBIINB")

        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertTrue(result.rawMerchant.contains("DMRC") || result.rawMerchant.contains("DELHI METRO"))
        assertEquals("429182749104", result.upiRefNo)
        assertEquals("SBI", result.bankOrAppSource)
    }

    @Test
    fun testIciciCreditCardDebitSms() {
        val sms = "Alert! You have spent INR 2,499.00 on your ICICI Bank Credit Card ending 8091 at AMAZON PAYMENTS on 24-Oct-26."
        val result = TransactionParser.parse(sms, senderOrPackage = "ICICIB")

        assertNotNull(result)
        assertEquals(2499.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("AMAZON PAYMENTS", result.rawMerchant)
        assertEquals("ICICI Bank", result.bankOrAppSource)
    }

    @Test
    fun testAxisBankUpiSms() {
        val sms = "Rs. 380.00 debited from Axis Bank A/c ending 9012 on 24-10-2026 to VPA zepto@axis (Zepto). UPI UTR: 429182749103."
        val result = TransactionParser.parse(sms, senderOrPackage = "AXISBK")

        assertNotNull(result)
        assertEquals(380.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Zepto", result.rawMerchant)
        assertEquals("zepto@axis", result.upiVpa)
        assertEquals("429182749103", result.upiRefNo)
        assertEquals("Axis Bank", result.bankOrAppSource)
    }

    @Test
    fun testKotakBankUpiP2pSms() {
        val sms = "Sent Rs 1250.00 from Kotak Bank A/c XX5678 to Suresh Yadav on 24-Oct-26. UPI Ref 429182749105."
        val result = TransactionParser.parse(sms, senderOrPackage = "KOTAKB")

        assertNotNull(result)
        assertEquals(1250.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Suresh Yadav", result.rawMerchant)
        assertEquals("429182749105", result.upiRefNo)
        assertEquals("Kotak Bank", result.bankOrAppSource)
    }

    @Test
    fun testGooglePayPushNotificationPaid() {
        val notification = "Paid ₹60 to Chai Point"
        val result = TransactionParser.parse(notification, senderOrPackage = "com.google.android.apps.nbu.paisa.user")

        assertNotNull(result)
        assertEquals(60.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Chai Point", result.rawMerchant)
        assertEquals("Google Pay", result.bankOrAppSource)
    }

    @Test
    fun testGooglePayPushNotificationYouPaid() {
        val notification = "You paid McDonald's ₹350"
        val result = TransactionParser.parse(notification, senderOrPackage = "com.google.android.apps.nbu.paisa.user")

        assertNotNull(result)
        assertEquals(350.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("McDonald's", result.rawMerchant)
        assertEquals("Google Pay", result.bankOrAppSource)
    }

    @Test
    fun testPhonePePushNotification() {
        val notification = "Paid ₹120 to Zepto"
        val result = TransactionParser.parse(notification, senderOrPackage = "com.phonepe.app")

        assertNotNull(result)
        assertEquals(120.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Zepto", result.rawMerchant)
        assertEquals("PhonePe", result.bankOrAppSource)
    }

    @Test
    fun testPaytmPushNotification() {
        val notification = "Sent ₹150 to Suresh Yadav using Paytm UPI"
        val result = TransactionParser.parse(notification, senderOrPackage = "net.one97.paytm")

        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, result.type)
        assertEquals("Suresh Yadav", result.rawMerchant)
        assertEquals("Paytm", result.bankOrAppSource)
    }

    @Test
    fun testHdfcRefundCreditSms() {
        val sms = "Dear Customer, INR 450.00 credited to A/c XX1234 on 24-OCT-26 by refund from Zomato. UPI Ref: 429182749110."
        val result = TransactionParser.parse(sms, senderOrPackage = "VM-HDFCBK")

        assertNotNull(result)
        assertEquals(450.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("Zomato", result.rawMerchant)
        assertEquals("429182749110", result.upiRefNo)
        assertEquals("HDFC Bank", result.bankOrAppSource)
    }

    @Test
    fun testSbiSalaryCreditSms() {
        val sms = "Your A/C ending 4567 credited by INR 85,000.00 on 24-OCT-26 by transfer from TechCorp Solutions. Ref 429182749111."
        val result = TransactionParser.parse(sms, senderOrPackage = "AX-SBIINB")

        assertNotNull(result)
        assertEquals(85000.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("TechCorp Solutions", result.rawMerchant)
        assertEquals("429182749111", result.upiRefNo)
        assertEquals("SBI", result.bankOrAppSource)
    }

    @Test
    fun testPhonePeReceivedMoneyNotification() {
        val notification = "Received ₹500 from Rahul"
        val result = TransactionParser.parse(notification, senderOrPackage = "com.phonepe.app")

        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, result.type)
        assertEquals("Rahul", result.rawMerchant)
        assertEquals("PhonePe", result.bankOrAppSource)
    }

    @Test
    fun testRejectionFilterOtpSms() {
        val sms = "847291 is your secret OTP for transaction of INR 1,200.00 at Swiggy. Do not share this OTP with anyone."
        val result = TransactionParser.parse(sms, senderOrPackage = "VM-HDFCBK")
        assertNull("OTP SMS must be immediately rejected", result)
    }

    @Test
    fun testRejectionFilterCreditCardBillReminder() {
        val sms = "Dear Customer, your HDFC Bank Credit Card bill of Rs. 18,450.00 is due on 28-OCT-26. Pay minimum amount due of Rs. 920.00 to avoid late charges."
        val result = TransactionParser.parse(sms, senderOrPackage = "VM-HDFCBK")
        assertNull("Credit card bill reminder must be rejected", result)
    }

    @Test
    fun testRejectionFilterPreApprovedLoanPromo() {
        val sms = "Congratulations! You have pre-approved personal loan of Rs 5,00,000 available at 10.5% interest. Click here to avail mandate."
        val result = TransactionParser.parse(sms, senderOrPackage = "AX-BAJAJ")
        assertNull("Promotional pre-approved loan SMS must be rejected", result)
    }

    @Test
    fun testRejectionFilterFailedTransaction() {
        val sms = "Your transaction of Rs 850.00 at Uber has failed due to insufficient funds in A/c XX1234."
        val result = TransactionParser.parse(sms, senderOrPackage = "VM-HDFCBK")
        assertNull("Failed transaction alerts must be rejected", result)
    }
}
