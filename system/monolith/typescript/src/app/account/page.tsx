"use client";

import { useEffect, useState, type FormEvent } from "react";
import { AccountLayout, LoadingSpinner } from "@/components/StoreLayout";
import { AddressFields } from "@/components/StorefrontParts";
import { useNotification } from "@/components/StorefrontProviders";
import { PROFILE_LABELS } from "@/lib/storefront/address-labels";
import { updateProfile, viewProfile } from "@/lib/storefront/api-client";
import { addressOf, EMPTY_ADDRESS } from "@/lib/storefront/shipping";
import type { ShippingAddress } from "@/lib/storefront/types";

/** `/account` (My account › Profile & address): name and default shipping address (UC-010). */
export default function AccountPage() {
  const { setSuccess, setError, clearNotification } = useNotification();
  const [form, setForm] = useState<ShippingAddress | null>(null);
  const [email, setEmail] = useState("");
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    let ignore = false;
    void viewProfile().then((result) => {
      if (ignore) return;
      if (result.success) {
        setForm(addressOf(result.data));
        setEmail(result.data.email ?? "");
      } else {
        setError({ message: result.message, fieldErrors: result.fieldErrors });
        setForm(EMPTY_ADDRESS);
      }
    });
    return () => {
      ignore = true;
    };
  }, [setError]);

  // The backend validates (required fields, spaces count as empty, US/DE/GB) and reports each field to correct.
  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!form) return;
    setIsSaving(true);
    const result = await updateProfile(form);
    setIsSaving(false);
    clearNotification();
    if (result.success) {
      setForm(addressOf(result.data));
      setEmail(result.data.email ?? "");
      setSuccess("Your profile has been saved.");
    } else {
      setError({ message: result.message, fieldErrors: result.fieldErrors });
    }
  };

  return (
    <AccountLayout title="My account" breadcrumbs={[{ label: "Home", path: "/" }, { label: "My account" }, { label: "Profile & address" }]}>
      <div className="card shadow">
        <div className="card-header bg-primary text-white">
          <h4 className="mb-0">Profile &amp; address</h4>
        </div>
        <div className="card-body">
          {form ? (
            <form noValidate onSubmit={(e) => void handleSubmit(e)}>
              <div className="row g-3 mb-3">
                <div className="col-md-6">
                  <label htmlFor="profile-firstName" className="form-label">
                    First name
                  </label>
                  <input
                    id="profile-firstName"
                    type="text"
                    className="form-control"
                    aria-label="First Name"
                    value={form.firstName}
                    onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  />
                </div>
                <div className="col-md-6">
                  <label htmlFor="profile-lastName" className="form-label">
                    Last name
                  </label>
                  <input
                    id="profile-lastName"
                    type="text"
                    className="form-control"
                    aria-label="Last Name"
                    value={form.lastName}
                    onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  />
                </div>
                <div className="col-12">
                  <label htmlFor="profile-email" className="form-label">
                    Email
                  </label>
                  <input id="profile-email" type="email" className="form-control" aria-label="Email" value={email} readOnly disabled />
                  <div className="form-text">Your email comes from your sign-in account and can&apos;t be changed here.</div>
                </div>
              </div>
              <h5 className="mt-4">Shipping address</h5>
              <AddressFields idPrefix="profile" address={form} errors={{}} labels={PROFILE_LABELS} onChange={setForm} withoutNames />
              <p className="form-text mt-3" aria-label="Tax Country Hint">
                Sales tax is charged at the rate of your shipping address&apos;s country.
              </p>
              <button type="submit" className="btn btn-primary" aria-label="Save Profile" disabled={isSaving}>
                {isSaving ? "Saving..." : "Save profile"}
              </button>
            </form>
          ) : (
            <LoadingSpinner message="Loading profile..." />
          )}
        </div>
      </div>
    </AccountLayout>
  );
}
