import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { ChevronRight, RefreshCw } from 'lucide-react';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  createColumnHelper,
  flexRender,
  type SortingState,
} from '@tanstack/react-table';
import { useRoles } from '../../auth/useRoles';
import { LoadingSpinner, ErrorMessage, SortableHeaderCell } from '../../components';
import { formatDateTime } from '../../i18n/format';
import type { BrowseOrderHistoryItemResponse } from '../../types/api.types';

export interface CustomerFilter {
  /** '' means All customers. */
  value: string;
  options: string[];
  onChange: (customer: string) => void;
}

export interface OrderHistoryTableProps {
  orders: BrowseOrderHistoryItemResponse[];
  filter: string;
  onFilterChange: (value: string) => void;
  /** Admins only (UC-011 BR-3); customers never get it. */
  customerFilter?: CustomerFilter;
  isLoading: boolean;
  error: string | null;
  onRefresh: () => void;
}

const columnHelper = createColumnHelper<BrowseOrderHistoryItemResponse>();

/**
 * Order history table component using TanStack Table
 * Includes sorting and order listing; filtering by order number happens server-side (useOrders)
 */
export function OrderHistoryTable({
  orders,
  filter,
  onFilterChange,
  customerFilter,
  isLoading,
  error,
  onRefresh
}: Readonly<OrderHistoryTableProps>) {
  const { t } = useTranslation('account');
  const [sorting, setSorting] = useState<SortingState>([]);
  const { isAdmin } = useRoles();
  // Only admins see everyone's orders, and only backends that report the owner can fill the column.
  const showCustomer = isAdmin && orders.some((order) => order.customer);

  const columns = useMemo(
    () => [
      columnHelper.accessor('orderNumber', {
        header: t('orderTable.columns.orderNumber'),
        cell: (info) => <span className="order-number">{info.getValue()}</span>,
      }),
      ...(showCustomer
        ? [
            columnHelper.accessor('customer', {
              header: t('orderTable.columns.customer'),
              cell: (info) => info.getValue() ?? '-',
            }),
          ]
        : []),
      columnHelper.accessor('orderTimestamp', {
        header: t('orderTable.columns.orderDate'),
        cell: (info) => formatDateTime(info.getValue()),
        sortingFn: 'datetime',
      }),
      columnHelper.accessor('sku', {
        header: t('orderTable.columns.sku'),
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('quantity', {
        header: t('orderTable.columns.quantity'),
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('country', {
        header: t('orderTable.columns.country'),
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('totalPrice', {
        header: t('orderTable.columns.totalPrice'),
        cell: (info) => <span className="order-total">${info.getValue()}</span>,
      }),
      columnHelper.accessor('appliedCouponCode', {
        header: t('orderTable.columns.couponCode'),
        cell: (info) => info.getValue() ?? '-',
      }),
      columnHelper.accessor('status', {
        header: t('orderTable.columns.status'),
        cell: (info) => (
          <span className={`order-status status-${info.getValue()}`}>{info.getValue()}</span>
        ),
      }),
      columnHelper.display({
        id: 'actions',
        header: t('orderTable.columns.actions'),
        // "View Details" must stay the link's first text node: drivers find it with contains(text(), ...).
        cell: (info) => (
          <Link to={`/order-details/${encodeURIComponent(info.row.original.orderNumber)}`} className="order-view-link">
            {t('orderTable.viewDetails')}
            <ChevronRight size={16} aria-hidden="true" />
          </Link>
        ),
      }),
    ],
    [showCustomer, t]
  );

  // eslint-disable-next-line react-hooks/incompatible-library -- TanStack Table's useReactTable is inherently incompatible with React Compiler memoization; no compiler is used here and there is no compatible API to switch to
  const table = useReactTable({
    data: orders,
    columns,
    state: {
      sorting,
    },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  const orderCount = orders.length;

  return (
    <>
      <div className="orders-toolbar">
        <div>
          <h1 className="h2 mb-1">{t('orderTable.heading')}</h1>
          {!isLoading && !error && (
            <p className="text-body-secondary mb-0">
              {t('orderTable.count', { count: orderCount })}
            </p>
          )}
        </div>
        <div className="orders-filters">
          {customerFilter && (
            <div className="orders-filter">
              <label htmlFor="customerFilter" className="form-label market-label">
                {t('orderTable.customerLabel')}
              </label>
              <select
                id="customerFilter"
                className="form-select"
                aria-label={t('orderTable.customerFilter')}
                value={customerFilter.value}
                onChange={(e) => customerFilter.onChange(e.target.value)}
              >
                <option value="">{t('orderTable.allCustomers')}</option>
                {customerFilter.options.map((customer) => (
                  <option key={customer} value={customer}>
                    {customer}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div className="orders-filter orders-filter-wide">
            <label htmlFor="orderNumberFilter" className="form-label market-label">
              {t('orderTable.filterLabel')}
            </label>
            <input
              type="text"
              className="form-control"
              id="orderNumberFilter"
              aria-label={t('orderTable.filterAria')}
              value={filter}
              onChange={(e) => onFilterChange(e.target.value)}
              placeholder={t('orderTable.filterPlaceholder')}
            />
          </div>
          <button
            type="button"
            className="btn btn-outline-secondary orders-refresh"
            onClick={onRefresh}
            disabled={isLoading}
            aria-label={t('orderTable.refreshAria')}
          >
            <RefreshCw size={16} aria-hidden="true" />
            {t('orderTable.refresh')}
          </button>
        </div>
      </div>

      {isLoading && (
        <div className="market-panel">
          <LoadingSpinner message={t('orderTable.loading')} />
        </div>
      )}
      {!isLoading && error && <ErrorMessage message={error} onRetry={onRefresh} />}
      {!isLoading && !error && (
        <div className="market-panel orders-panel">
          {/* The table keeps all its columns: on small screens the panel scrolls sideways, the page never does. */}
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0 orders-table">
              <thead>
                {table.getHeaderGroups().map((headerGroup) => (
                  <tr key={headerGroup.id}>
                    {headerGroup.headers.map((header) => (
                      <SortableHeaderCell key={header.id} header={header} />
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody>
                {table.getRowModel().rows.length === 0 ? (
                  <tr>
                    <td colSpan={columns.length} className="orders-empty text-center">
                      {t('orderTable.empty')}
                    </td>
                  </tr>
                ) : (
                  table.getRowModel().rows.map((row) => (
                    <tr key={row.id}>
                      {row.getVisibleCells().map((cell) => (
                        <td key={cell.id} className={`orders-cell-${cell.column.id}`}>
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  );
}
