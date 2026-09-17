import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  createColumnHelper,
  flexRender,
  type SortingState,
} from '@tanstack/react-table';
import { LoadingSpinner, ErrorMessage, SortableHeaderCell } from '../../components';
import type { BrowseOrderHistoryItemResponse } from '../../types/api.types';

export interface OrderHistoryTableProps {
  orders: BrowseOrderHistoryItemResponse[];
  filter: string;
  onFilterChange: (value: string) => void;
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
  isLoading,
  error,
  onRefresh
}: Readonly<OrderHistoryTableProps>) {
  const [sorting, setSorting] = useState<SortingState>([]);

  const columns = useMemo(
    () => [
      columnHelper.accessor('orderNumber', {
        header: 'Order Number',
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('orderTimestamp', {
        header: 'Order Date',
        cell: (info) => new Date(info.getValue()).toLocaleString('en-US', { timeZone: 'UTC' }),
        sortingFn: 'datetime',
      }),
      columnHelper.accessor('sku', {
        header: 'SKU',
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('quantity', {
        header: 'Quantity',
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('country', {
        header: 'Country',
        cell: (info) => info.getValue(),
      }),
      columnHelper.accessor('totalPrice', {
        header: 'Total Price',
        cell: (info) => `$${info.getValue().toFixed(2)}`,
      }),
      columnHelper.accessor('appliedCouponCode', {
        header: 'Coupon Code',
        cell: (info) => info.getValue() ?? '-',
      }),
      columnHelper.accessor('status', {
        header: 'Status',
        cell: (info) => (
          <span className={`status-${info.getValue()}`}>
            {info.getValue()}
          </span>
        ),
      }),
      columnHelper.display({
        id: 'actions',
        header: 'Actions',
        cell: (info) => (
          <Link to={`/order-details/${encodeURIComponent(info.row.original.orderNumber)}`}>
            View Details
          </Link>
        ),
      }),
    ],
    []
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

  return (
    <div className="card shadow">
      <div className="card-header bg-primary text-white">
        <h4 className="mb-0">Order History</h4>
      </div>
      <div className="card-body">
        <div className="row mb-3">
          <div className="col-md-8">
            <label htmlFor="orderNumberFilter" className="form-label">
              Filter by Order Number:
            </label>
            <input
              type="text"
              className="form-control"
              id="orderNumberFilter"
              aria-label="Order Number"
              value={filter}
              onChange={(e) => onFilterChange(e.target.value)}
              placeholder="Enter order number..."
            />
          </div>
          <div className="col-md-4 d-flex align-items-end">
            <button
              className="btn btn-secondary w-100"
              onClick={onRefresh}
              disabled={isLoading}
              aria-label="Refresh Order List"
            >
              Refresh
            </button>
          </div>
        </div>

        {isLoading && <LoadingSpinner message="Loading orders..." />}
        {!isLoading && error && <ErrorMessage message={error} onRetry={onRefresh} />}
        {!isLoading && !error && (
          <div className="table-responsive">
            <table className="table table-striped table-hover">
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
                    <td colSpan={columns.length} className="text-center">
                      No orders found
                    </td>
                  </tr>
                ) : (
                  table.getRowModel().rows.map((row) => (
                    <tr key={row.id}>
                      {row.getVisibleCells().map((cell) => (
                        <td key={cell.id}>
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
