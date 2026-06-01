import LoanListPage from '@/components/loan/LoanListPage'

export default function MortgageLoanPage() {
  return <LoanListPage loanTypeCd="MORTGAGE" pageTitle="담보대출" activeHref="/products/loan/mortgage" />
}
