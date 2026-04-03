import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  useReactTable,
  getCoreRowModel,
  getSortedRowModel,
  getFilteredRowModel,
  createColumnHelper,
  flexRender,
  type SortingState,
  type ColumnFiltersState,
} from '@tanstack/react-table';
import { LoadingSpinner, ErrorMessage } from '../../components';
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
 * Includes sorting, filtering, and order listing
 */
export function OrderHistoryTable({
  orders,
  filter,
  onFilterChange,
  isLoading,
  error,
  onRefresh
}: OrderHistoryTableProps) {
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);

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
      columnHelper.accessor('totalPrice', {
        header: 'Total Price',
        cell: (info) => `$${info.getValue().toFixed(2)}`,
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

  // Filter orders by order number
  const filteredOrders = useMemo(() => {
    if (!filter) return orders;
    return orders.filter((order) =>
      order.orderNumber.toLowerCase().includes(filter.toLowerCase())
    );
  }, [orders, filter]);

  const table = useReactTable({
    data: filteredOrders,
    columns,
    state: {
      sorting,
      columnFilters,
    },
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
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
                      <th
                        key={header.id}
                        onClick={header.column.getToggleSortingHandler()}
                        style={{ cursor: header.column.getCanSort() ? 'pointer' : 'default' }}
                      >
                        <div className="d-flex align-items-center">
                          {flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                          {header.column.getIsSorted() && (
                            <span className="ms-1">
                              {header.column.getIsSorted() === 'asc' ? '\u2191' : '\u2193'}
                            </span>
                          )}
                        </div>
                      </th>
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
