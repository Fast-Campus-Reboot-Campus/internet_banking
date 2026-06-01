import LoanListPage from '@/components/loan/LoanListPage'

export default function CreditLoanPage() {
  return <LoanListPage loanTypeCd="CREDIT" pageTitle="신용대출" activeHref="/products/loan/credit" />
}
